package cn.edu.mju.band.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 文档元信息（实验环节二）。
 *
 * <p>springdoc 会扫描所有 {@code @RestController} 上的 {@code @Tag} / {@code @Operation} /
 * {@code @Schema} 生成 {@code /v3/api-docs}（OpenAPI 3.0 JSON），
 * 本类只补充"整个文档级别"的信息：标题、版本、联系人、许可证、服务地址。</p>
 *
 * <p><b>与手写文档的关系</b>：接口形状（参数、字段、错误码）由代码注解生成，
 * 改代码即改文档，不存在"文档写了但代码没实现"；而
 * {@code docs/API.md} 负责讲清"为什么这样设计、错误码怎么处理"，
 * 那些是注解表达不了的内容。两者一份给机器读，一份给人读。</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI taskOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("极简任务系统 API")
                        .version("v1")
                        .description("""
                                任务管理系统的对外 HTTP 接口。

                                **统一响应**：所有接口返回 `{code, message, data, timestamp}`，
                                `code=0` 表示成功；失败时 HTTP 状态码与 `code` 前三位一致。

                                **认证**：接口复用页面登录后的会话（Cookie: JSESSIONID）。
                                未登录调用会返回 401，请先在 `/login` 登录，或使用浏览器打开本文档
                                （同源请求会自动带上 Cookie）。

                                **日期**：所有日期字段均为 `yyyy-MM-dd` 字符串。
                                """)
                        // 说明：演示环境地址随 profile 变化（dev 8088 / test 8089 / prod 8080），
                        // 用相对地址 "/" 让文档在任意端口都指向自己，而不是写死 localhost:8088。
                        .contact(new Contact().name("MJ 软件工程课程组").email("dev@example.edu.cn"))
                        // 与仓库根目录 LICENSE 保持一致：木兰宽松许可证第 2 版
                        .license(new License().name("Mulan PSL v2")
                                .url("http://license.coscl.org.cn/MulanPSL2")))
                .servers(List.of(new Server().url("/").description("当前服务")));
    }
}
