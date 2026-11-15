package cn.edu.mju.band.controller;

import cn.edu.mju.band.buss.IUserBuss;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** LoginController 纯单元测试：业务层与 HttpSession 全部 Mock。 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LoginController 单元测试")
class LoginControllerTest {

    @Mock
    private IUserBuss userBuss;

    @Mock
    private HttpSession session;

    @InjectMocks
    private LoginController controller;

    @Test
    @DisplayName("首页 / 登录页返回正确视图")
    void shouldRenderStaticPages() {
        assertThat(controller.index()).isEqualTo("index");
        assertThat(controller.loginPage()).isEqualTo("login");
    }

    @Test
    @DisplayName("登录成功：写入 session 标记并跳转任务页")
    void doLoginShouldWriteSessionWhenAuthenticated() {
        when(userBuss.login("admin", "123456")).thenReturn(true);

        assertThat(controller.doLogin("admin", "123456", session))
                .isEqualTo("redirect:/task");
        verify(session).setAttribute("flag", true);
        verify(session).setAttribute("loginName", "admin");
    }

    @Test
    @DisplayName("登录失败：不写 session，退回登录页")
    void doLoginShouldNotWriteSessionWhenRejected() {
        when(userBuss.login("admin", "bad")).thenReturn(false);

        assertThat(controller.doLogin("admin", "bad", session))
                .isEqualTo("redirect:/login");
        verify(session, never()).setAttribute(anyString(), any());
    }

    @Test
    @DisplayName("注销：销毁 session 并退回登录页")
    void logoutShouldInvalidateSession() {
        assertThat(controller.logout(session)).isEqualTo("redirect:/login");
        verify(session).invalidate();
    }
}
