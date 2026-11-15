package cn.edu.mju.band.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 统一异常处理（实验环节四）。
 *
 * <p>表单校验错误由 {@code TaskController.save()} 里的 {@code BindingResult} 就地回显，
 * 本类负责兜住「根本没进到 Controller 方法」的异常：路径参数类型转换失败
 * （如 {@code /task?opt=update&id=abc}）、方法级校验违规、非法参数等，
 * 以及其余未预期的异常。统一转成友好错误页，避免把 500 堆栈直接暴露给用户。</p>
 *
 * <p>静态资源 404（NoResourceFoundException）原样交回容器，保持 404 语义，
 * 避免被全局兜底误当成系统错误。</p>
 *
 * <p><b>为什么限定 basePackages</b>（实验环节二补）：{@link cn.edu.mju.band.api.ApiExceptionHandler}
 * 也声明了 {@code @ExceptionHandler(Exception.class)} 兜底，两个建议器同时匹配时
 * 谁生效取决于 bean 注册顺序——改一行 import 就可能让接口返回 HTML 错误页。
 * 用 basePackages 把边界写成显式规则：本类只管页面控制器（controller 包），
 * 接口控制器（api 包）由 ApiExceptionHandler 负责。</p>
 */
@ControllerAdvice(basePackages = "cn.edu.mju.band.controller")
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            BindException.class,
            ConstraintViolationException.class,
            IllegalArgumentException.class
    })
    public String handleBadRequest(Exception e, Model model) {
        log.warn("请求参数不合法: {}", e.getMessage());
        model.addAttribute("errorMessage", "请求参数不合法，请检查后重试（" + brief(e) + "）");
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleUnexpected(Exception e, Model model) throws Exception {
        // 静态资源找不到不该变成系统错误页，交回 Spring Boot 默认 404 处理
        if ("NoResourceFoundException".equals(e.getClass().getSimpleName())) {
            throw e;
        }
        log.error("未预期的异常", e);
        model.addAttribute("errorMessage", "系统繁忙，请稍后重试");
        return "error";
    }

    private String brief(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isEmpty()) {
            return e.getClass().getSimpleName();
        }
        return msg.length() > 120 ? msg.substring(0, 120) + "…" : msg;
    }
}
