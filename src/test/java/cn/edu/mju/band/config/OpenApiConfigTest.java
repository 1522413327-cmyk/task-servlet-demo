package cn.edu.mju.band.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 文档元信息测试。
 *
 * <p>看似只是几个常量，但标题/版本一旦为空，Swagger UI 顶部就会显示成
 * "OpenAPI definition"——答辩时被问"你这是哪个系统"就很尴尬。
 * 用一个用例把契约固定下来。</p>
 */
@DisplayName("OpenAPI 文档配置测试")
class OpenApiConfigTest {

    @Test
    @DisplayName("文档标题、版本、服务地址均已设置")
    void openApiInfoShouldBeComplete() {
        OpenAPI api = new OpenApiConfig().taskOpenApi();

        assertThat(api.getInfo().getTitle()).isEqualTo("极简任务系统 API");
        assertThat(api.getInfo().getVersion()).isEqualTo("v1");
        assertThat(api.getInfo().getDescription()).contains("统一响应").contains("认证");
        assertThat(api.getServers()).hasSize(1);
        assertThat(api.getServers().get(0).getUrl()).isEqualTo("/");
    }
}
