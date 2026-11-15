package cn.edu.mju.band.buss.impl;

import cn.edu.mju.band.buss.ITaskBuss;
import cn.edu.mju.band.entity.PageResult;
import cn.edu.mju.band.entity.Task;
import cn.edu.mju.band.mapper.TaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 任务业务实现。
 * 原来在 applicationContext.xml 里用 &lt;bean id="taskBuss"&gt; 注册，
 * 现在改成 @Service，由 Spring Boot 的 @ComponentScan 自动扫描进容器（bean 名默认 taskBuss）。
 *
 * <p>数据访问全部走 BaseMapper + Wrapper + Page，不再需要手写 SQL（对照实现见 TaskMapper）。</p>
 *
 * <p><b>改造说明（实验环节二 / 五）</b>：分页的 5 / 100 两个魔法数字已外置到
 * {@code app.task.default-page-size} / {@code app.task.max-page-size}，
 * 由 application-{profile}.yml 按环境注入；同时补上 null、非法 id、
 * 数据层异常三类原先未覆盖的分支。</p>
 */
@Service
public class TaskBuss implements ITaskBuss {

    private static final Logger log = LoggerFactory.getLogger(TaskBuss.class);

    // 依赖由 Spring 通过 setter 注入（由 @MapperScan 生成的 TaskMapper 代理）
    private TaskMapper taskMapper;

    /**
     * 业务参数来自当前激活的 profile。
     * 字段初始化值保证「脱离 Spring 容器」的纯单元测试（JUnit + Mockito）行为与默认环境一致，
     * Spring 启动时再由 @Value 按 profile 覆盖 —— 这是多环境参数真正生效的地方。
     */
    @Value("${app.task.default-page-size:5}")
    private int defaultPageSize = 5;

    @Value("${app.task.max-page-size:100}")
    private int maxPageSize = 100;

    @Autowired
    public void setTaskMapper(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    /** 供测试显式模拟不同环境的业务参数；不用 setXxx 命名，避免被 Mockito 当成注入点。 */
    public void applyPaginationConfig(int defaultPageSize, int maxPageSize) {
        this.defaultPageSize = defaultPageSize;
        this.maxPageSize = maxPageSize;
    }

    /**
     * 查全部任务。用 Wrapper 表达排序条件，
     * 等价于原来的 {@code SELECT ... FROM t_task ORDER BY id DESC}。
     * 写操作不降级：Mapper 异常原样上抛，让调用方明确知道失败了。
     */
    @Override
    public List<Task> list() {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .orderByDesc(Task::getId);
        return taskMapper.selectList(wrapper);
    }

    @Override
    public boolean add(Task task) {
        if (task == null) {
            return false;
        }
        // insert 由 BaseMapper 提供：字段按非 null 拼进 INSERT，自增主键回填到 task.id
        return taskMapper.insert(task) > 0;
    }

    @Override
    public boolean update(Task task) {
        // updateById 依赖主键；缺 id 时直接拦下，避免生成 UPDATE ... WHERE id IS NULL
        if (task == null || task.getId() == null) {
            return false;
        }
        return taskMapper.updateById(task) > 0;
    }

    @Override
    public Task findById(int id) {
        if (id <= 0) {
            return null;
        }
        return taskMapper.selectById(id);
    }

    @Override
    public boolean delete(int id) {
        if (id <= 0) {
            return false;
        }
        return taskMapper.deleteById(id) > 0;
    }

    /**
     * 条件 + 分页查询。
     *
     * <p>改造前：业务层自己算 offset、先 count 再 search，两次 SQL 都由 XML 手写。</p>
     * <p>改造后：条件交给 Wrapper，分页交给 {@link Page}，
     * COUNT 和 LIMIT 由 PaginationInnerInterceptor 自动生成。</p>
     *
     * <p>业务层保留四件必须自己做的事：条件清洗、参数兜底、越界修正、读操作异常降级。
     * 这些都是业务规则，不该交给框架。</p>
     */
    @Override
    public PageResult<Task> search(String title, Byte status, int pageNo, int pageSize) {
        // 1) 条件清洗：空白标题视为“不限”
        title = (title == null || title.trim().isEmpty()) ? null : title.trim();

        // 2) 参数兜底：魔法数字改为读 profile 配置
        if (pageSize < 1 || pageSize > maxPageSize) {
            pageSize = defaultPageSize;
        }
        if (pageNo < 1) {
            pageNo = 1;
        }

        // 3) 条件与排序交给 Wrapper：condition 参数为 false 时该条件不会拼进 WHERE
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .like(title != null, Task::getTitle, title)
                .eq(status != null, Task::getStatus, status)
                .orderByDesc(Task::getId);

        // 4) Page 只是参数容器：current=第几页，size=每页几条
        Page<Task> page = new Page<>(pageNo, pageSize);

        // 5) 读操作降级：数据库宕机 / 断网时，列表页显示「共 0 条」而不是 500，
        //    且不进行第二次查询、不返回半截数据（环节四的极端场景要求）
        try {
            Page<Task> result = taskMapper.selectPage(page, wrapper);

            // 6) 越界修正：请求页 > 总页数时回到最后一页重查
            if (result.getPages() > 0 && result.getCurrent() > result.getPages()) {
                page.setCurrent(result.getPages());
                result = taskMapper.selectPage(page, wrapper);
            }

            // 7) 转成项目自己的 PageResult，Controller 和页面完全不用改
            return new PageResult<>(result.getRecords(),
                    (int) result.getCurrent(),
                    (int) result.getSize(),
                    (int) result.getTotal());
        } catch (RuntimeException e) {
            log.error("任务分页查询失败，已降级为空结果: title={}, status={}, pageNo={}",
                    title, status, pageNo, e);
            return new PageResult<>(Collections.<Task>emptyList(), 1, pageSize, 0);
        }
    }
}
