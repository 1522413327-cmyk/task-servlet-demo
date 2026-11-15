package cn.edu.mju.band.controller;

import cn.edu.mju.band.buss.IUserBuss;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 登录控制器（SpringMVC）。
 * - GET  /       -> 首页 index.html
 * - GET  /login  -> 显示登录页 login.html
 * - POST /login  -> 校验账号密码，成功后写入 session 标记并跳转
 * - GET  /logout -> 注销
 *
 * 拦截规则见 {@link cn.edu.mju.band.config.WebConfig} 中注册的 LoginInterceptor（保护 /task，未登录跳回 /login）。
 */
@Controller
public class LoginController {

    @Autowired
    private IUserBuss userBuss;

    /** 首页（替代原来的 index.jsp） */
    @GetMapping("/")
    public String index() {
        return "index";                                   // -> classpath:/templates/index.html
    }

    /** 显示登录页 */
    @GetMapping("/login")
    public String loginPage() {
        return "login";                                   // -> classpath:/templates/login.html
    }

    /** 处理登录提交 */
    @PostMapping("/login")
    public String doLogin(@RequestParam("username") String username,
                          @RequestParam("password") String password,
                          HttpSession session) {
        if (userBuss.login(username, password)) {
            // 登录成功：在 session 写入标记，供拦截器识别
            session.setAttribute("flag", true);
            session.setAttribute("loginName", username);
            return "redirect:/task";                      // 登录后进入任务页（Spring 自动加 contextPath）
        }
        // 登录失败：回到登录页
        return "redirect:/login";
    }

    /** 注销 */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
