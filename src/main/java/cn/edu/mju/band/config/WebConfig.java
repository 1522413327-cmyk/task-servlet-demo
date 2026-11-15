package cn.edu.mju.band.config;

import cn.edu.mju.band.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvc 扩展配置。
 *
 * <p>原项目在 spring-mvc.xml 里用 {@code <mvc:interceptors>} 注册登录拦截器；
 * Spring Boot 里没有 XML，改成实现 WebMvcConfigurer 用 Java 代码注册，效果完全一致。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    // 构造器注入：LoginInterceptor 已加 @Component，由 Spring 容器提供
    public WebConfig(LoginInterceptor loginInterceptor) {
        this.loginInterceptor = loginInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                // 页面与接口共用一套登录态：两处都必须拦，否则接口会成为绕过登录的后门
                .addPathPatterns("/task", "/task/**", "/api/**");   // 等价于原来的 <mvc:mapping path="/task"/> 与 /task/**
    }
}
