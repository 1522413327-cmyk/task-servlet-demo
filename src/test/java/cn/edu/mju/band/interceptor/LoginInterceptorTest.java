package cn.edu.mju.band.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 登录拦截器单元测试（实验环节二）。
 *
 * <p>这个类只有 23 行，却是整个系统的安全边界：它一旦漏掉 /api/**，
 * 未登录的人就能直接调接口增删任务。所以四种分支都要有用例：
 * 已登录放行、页面未登录跳转、接口未登录返回 401 JSON、跨域预检放行。</p>
 *
 * <p>用 Spring 的 MockHttpServletRequest/Response，不启动容器，也不依赖数据库。</p>
 */
@DisplayName("登录拦截器单元测试")
class LoginInterceptorTest {

    private final LoginInterceptor interceptor = new LoginInterceptor(new ObjectMapper());

    private MockHttpServletRequest request(String method, String uri) {
        return new MockHttpServletRequest(method, uri);
    }

    @Test
    @DisplayName("已登录：放行，不改响应")
    void loggedInShouldPass() throws Exception {
        MockHttpServletRequest req = request("GET", "/task");
        req.getSession().setAttribute("flag", true);
        MockHttpServletResponse resp = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(req, resp, new Object())).isTrue();
        assertThat(resp.getStatus()).isEqualTo(200);
        assertThat(resp.getRedirectedUrl()).isNull();
    }

    @Test
    @DisplayName("未登录访问页面：302 到 /login")
    void anonymousPageRequestShouldRedirect() throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request("GET", "/task"), resp, new Object())).isFalse();
        assertThat(resp.getStatus()).isEqualTo(302);
        assertThat(resp.getRedirectedUrl()).isEqualTo("/login");
    }

    @Test
    @DisplayName("未登录调接口：401 + 统一 JSON，而不是跳转登录页")
    void anonymousApiRequestShouldReturn401Json() throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request("GET", "/api/v1/tasks"), resp, new Object())).isFalse();
        assertThat(resp.getStatus()).isEqualTo(401);
        assertThat(resp.getContentType()).isEqualTo("application/json;charset=UTF-8");
        assertThat(resp.getContentAsString())
                .contains("\"code\":40101")
                .contains("未登录或会话已过期");
    }

    @Test
    @DisplayName("部署在 contextPath 下：/app/api/** 同样识别为接口")
    void apiRequestUnderContextPathShouldBeRecognized() throws Exception {
        MockHttpServletRequest req = request("GET", "/app/api/v1/tasks");
        req.setContextPath("/app");
        MockHttpServletResponse resp = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(req, resp, new Object())).isFalse();
        assertThat(resp.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("跨域预检 OPTIONS：未登录也放行，否则浏览器认为接口不可用")
    void optionsRequestShouldPassWithoutLogin() throws Exception {
        MockHttpServletResponse resp = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request("OPTIONS", "/api/v1/tasks"), resp, new Object())).isTrue();
        assertThat(resp.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("会话存在但 flag 非 true：按未登录处理")
    void sessionWithoutFlagShouldBeRejected() throws Exception {
        MockHttpServletRequest req = request("GET", "/api/v1/tasks");
        req.getSession().setAttribute("loginName", "admin");   // 只有登录名，没有 flag
        MockHttpServletResponse resp = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(req, resp, new Object())).isFalse();
        assertThat(resp.getStatus()).isEqualTo(401);
    }
}
