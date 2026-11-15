package cn.edu.mju.band.mapper;

import cn.edu.mju.band.entity.Task;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务表 Mapper（取代原 ITaskDal / TaskDal）。
 *
 * <p>继承 {@link BaseMapper} 后，MyBatis-Plus 在启动时用动态代理补齐十多个通用方法，
 * 这些方法既不需要写 XML，也不需要写实现类：</p>
 * <pre>
 *   insert(entity)              新增，自增主键自动回填
 *   deleteById(id)              按主键删除
 *   deleteBatchIds(idList)      批量删除
 *   updateById(entity)          按主键更新（默认只更新非 null 字段）
 *   selectById(id)              按主键查询
 *   selectList(wrapper)         条件查询列表
 *   selectOne(wrapper)          查一条
 *   selectCount(wrapper)        条件计数
 *   selectPage(page, wrapper)   物理分页（COUNT + LIMIT 由分页插件生成）
 * </pre>
 *
 * <p>所以这个接口现在一个方法都不用声明：条件用 LambdaQueryWrapper 表达，
 * 分页用 Page，全部由 BaseMapper 提供，见 TaskBuss。它与 UserMapper 一样是空接口，
 * 但已经被 MyBatis 代理成完整可用的 Bean。</p>
 *
 * <p><b>命名禁忌</b>：不要再声明 insert / delete / update / selectById 这类与
 * BaseMapper 同名的方法。MyBatis 的 statement id 全局限定在 namespace 内，
 * 重名会让 MP 跳过注入，导致 Wrapper 版本的方法在调用时报
 * “Mapped Statements collection does not contain value”。</p>
 */
public interface TaskMapper extends BaseMapper<Task> {

    /* ===== 以下两个方法已停用，仅保留签名供对照 =====
     *
     * 它们原先由 mapper/TaskMapper.xml 的 searchWhere / search / count 三段手写 SQL 实现，
     * 业务层的用法是：先调 count 算总条数，再调 search 取当页数据，
     * offset = (pageNo - 1) * pageSize 由业务层自己算。
     *
     * 改造后 TaskBuss#search 改用 selectPage + PaginationInnerInterceptor，
     * COUNT 和 LIMIT 由插件自动生成，这两个方法已无任何调用方。
     *
     * 那份 XML 现已改名为 mapper/TaskMapper.bsk，不再被 mybatis-plus.mapper-locations
     * 的 classpath:mapper/*.xml 匹配，因此下面若放开声明，运行时会抛
     * BindingException: Invalid bound statement (not found)。
     *
     * 需要恢复手写分页时，两步一起做：
     *   1) mv mapper/TaskMapper.bsk mapper/TaskMapper.xml
     *   2) 放开下面两行方法声明（import 的 List 和 @Param 已在文件顶部保留）
     *
     * List<Task> search(@Param("title") String title,
     *                   @Param("status") Byte status,
     *                   @Param("offset") int offset,
     *                   @Param("pageSize") int pageSize);
     *
     * int count(@Param("title") String title, @Param("status") Byte status);
     */
}
