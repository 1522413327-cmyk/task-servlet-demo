package cn.edu.mju.band.interceptor;

import cn.edu.mju.band.common.ErrorCode;
import cn.edu.mju.band.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 登录拦截器（SpringMVC 版“过滤器”，对应视频里的 AuthFilter）。
 *
 * <p>作用：在请求到达 Controller 之前判断用户是否已登录，未登录则拦截。
 * 拦截路径在 {@link cn.edu.mju.band.config.WebConfig} 中注册（原 spring-mvc.xml 的 &lt;mvc:interceptors&gt;）。</p>
 *
 * <p><b>同一把锁，两种拒绝方式</b>（实验环节二补）：</p>
 * <ul>
 *   <li>页面请求（/task/**）：302 重定向到登录页，用户看到的是登录表单；</li>
 *   <li>接口请求（/api/**）：401 + 统一 JSON 响应体。若也返回 302，
 *       前端的 fetch/XHR 会拿到一个登录页的 HTML，报错信息完全看不懂 ——
 *       这正是前后端分离项目里“接口返回 200 但内容是登录页”的常见根因。</li>
 * </ul>
 *
 * <p>为什么要放行 OPTIONS：跨域预检请求不会携带 Cookie，若把它拦掉，
 * 浏览器会认为接口不可用，真正的业务请求根本发不出去。</p>
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    public LoginInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        // 跨域预检：浏览器不会带 Cookie，放行交给 CORS 处理
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        HttpSession session = request.getSession(false);
        Boolean flag = (session != null) ? (Boolean) session.getAttribute("flag") : null;
        if (Boolean.TRUE.equals(flag)) {
            // 已登录：放行
            return true;
        }

        if (isApiRequest(request)) {
            // 接口：401 + JSON，前端拦截器可据此统一跳登录页
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(), Result.fail(ErrorCode.UNAUTHORIZED));
            return false;
        }

        // 页面：重定向到登录页（内嵌 Tomcat 默认 contextPath 为空，/login 直接可用）
        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }

    /** 判断是否接口请求。先去掉 contextPath，避免部署到 /app 这类上下文路径时判断失效 */
    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath))
                ? uri.substring(contextPath.length())
                : uri;
        return path.startsWith("/api/");
    }
}
