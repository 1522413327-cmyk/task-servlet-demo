package cn.edu.mju.band.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件配置。
 *
 * <p><b>为什么必须配这一层</b>：{@code Page} 对象本身只是个“参数容器”，它不会自动变成 SQL。
 * 真正把 {@code new Page<>(2, 5)} 翻译成 {@code LIMIT 5, 5} 的，是
 * {@link PaginationInnerInterceptor}。</p>
 *
 * <p>如果只写 {@code selectPage} 而不注册拦截器，结果是<b>静默错误</b>：
 * SQL 里没有 LIMIT，把全表都查出来，而且 {@code page.getTotal()} 恒为 0 ——
 * 页面会出现“列表有 5 条数据，却显示共 0 条、共 0 页”的诡异现象。
 * 这类问题不报异常，只能靠看 SQL 日志发现。</p>
 *
 * <p>另一个容易踩的点：{@code PaginationInnerInterceptor} 从 MP 3.5.9 起被拆到
 * {@code mybatis-plus-jsqlparser} 模块（因为它要用 JSqlParser 改写 SQL）。
 * 只引 starter 不引 jsqlparser，会在启动时报 ClassNotFoundException。
 * 本项目 pom 里两个依赖都在，正是这个原因。</p>
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);

        // 单页上限：即使前端把 pageSize 改成 100000，最多也只查 100 条，防止一次拉爆数据库
        pagination.setMaxLimit(100L);

        // false：页码越界时不被拦截器自动改写，交给业务层自己决定（本项目是“回到最后一页”）。
        // 设成 true 的话，MP 会把越界页码直接改成第 1 页并重新查询，业务层就拿不到原始请求页码了。
        pagination.setOverflow(false);

        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
