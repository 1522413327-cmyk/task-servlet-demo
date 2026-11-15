package cn.edu.mju.band.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 环节四：统一异常处理的纯单元测试，验证异常被转成友好错误页而不是 500 堆栈。
 */
@DisplayName("GlobalExceptionHandler 统一异常处理")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private Model model;

    @BeforeEach
    void setUp() {
        model = new ExtendedModelMap();
    }

    @Test
    @DisplayName("参数类异常返回 error 视图并带友好提示")
    void badRequestShouldReturnErrorView() {
        String view = handler.handleBadRequest(
                new IllegalArgumentException("id 必须是数字"), model);

        assertThat(view).isEqualTo("error");
        assertThat(model.asMap().get("errorMessage").toString())
                .contains("请求参数不合法")
                .contains("id 必须是数字");
    }

    @Test
    @DisplayName("异常消息为空时退化为异常类名，不出现 null")
    void blankMessageShouldFallbackToClassName() {
        handler.handleBadRequest(new IllegalArgumentException(), model);
        assertThat(model.asMap().get("errorMessage").toString())
                .contains("IllegalArgumentException")
                .doesNotContain("null");
    }

    @Test
    @DisplayName("未预期异常返回通用提示，不泄露堆栈")
    void unexpectedShouldReturnGenericMessage() throws Exception {
        String view = handler.handleUnexpected(
                new RuntimeException("Connection refused"), model);

        assertThat(view).isEqualTo("error");
        assertThat(model.asMap().get("errorMessage")).isEqualTo("系统繁忙，请稍后重试");
    }

    @Test
    @DisplayName("静态资源 404 原样抛出，保持 404 语义")
    void noResourceFoundShouldBeRethrown() {
        // 用同名嵌套类模拟 Spring 的 NoResourceFoundException（按 SimpleName 判断）
        assertThatThrownBy(() -> handler.handleUnexpected(new NoResourceFoundException(), model))
                .isInstanceOf(NoResourceFoundException.class);
        assertThat(model.asMap()).doesNotContainKey("errorMessage");
    }

    /** 仅用于让 getSimpleName() 返回与 Spring 静态资源异常一致的名字。 */
    private static class NoResourceFoundException extends RuntimeException {
    }
}
