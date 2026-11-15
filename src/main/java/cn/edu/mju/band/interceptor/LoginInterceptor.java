package cn.edu.mju.band.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器（SpringMVC 版“过滤器”，对应视频里的 AuthFilter）。
 *
 * <p>作用：在请求到达 Controller 之前判断用户是否已登录，未登录则重定向到 /login。
 * 拦截路径在 {@link cn.edu.mju.band.config.WebConfig} 中注册（原 spring-mvc.xml 的 &lt;mvc:interceptors&gt;）。
 *
 * <p>Spring Boot 里加 @Component 交给容器管理，之后 WebConfig 直接注入即可。
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        Boolean flag = (session != null) ? (Boolean) session.getAttribute("flag") : null;
        if (Boolean.TRUE.equals(flag)) {
            // 已登录：放行
            return true;
        }
        // 未登录：重定向到登录页（内嵌 Tomcat 默认 contextPath 为空，/login 直接可用）
        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }
}
