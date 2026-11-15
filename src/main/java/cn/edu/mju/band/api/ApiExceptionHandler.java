package cn.edu.mju.band.api;

import cn.edu.mju.band.common.ApiException;
import cn.edu.mju.band.common.ErrorCode;
import cn.edu.mju.band.common.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/**
 * REST 接口异常处理器（实验环节二）。
 *
 * <p><b>为什么要和 {@code GlobalExceptionHandler} 分开</b>：那个类返回的是视图名
 * {@code "error"}，由 Thymeleaf 渲染 HTML 错误页；接口必须返回 JSON。
 * 两个建议器都用 {@code @ExceptionHandler(Exception.class)} 兜底时，
 * 谁生效取决于 bean 的注册顺序 —— 这是隐式契约，改一行 import 就可能变。
 * 所以这里用 {@code basePackages} 把"页面的归页面、接口的归接口"写成显式规则：
 * 本类只处理 {@code cn.edu.mju.band.api} 包下的控制器，
 * {@code GlobalExceptionHandler} 只处理 {@code cn.edu.mju.band.controller} 包。</p>
 *
 * <p><b>为什么 HTTP 状态码和业务码都要给</b>：HTTP 状态码让网关、监控、
 * 浏览器 DevTools 一眼看出成败（404 的请求不会进业务告警）；
 * 业务码让前端区分"任务不存在"和"参数不合法"这两种同样返回 4xx 的情况。</p>
 */
@RestControllerAdvice(basePackages = "cn.edu.mju.band.api")
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** 业务层主动抛出的异常，错误码与文案都已在异常里带好 */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Result<Void>> handleApiException(ApiException e) {
        ErrorCode code = e.getErrorCode();
        // 5xx 才记 error（要有人管），4xx 是调用方的问题，warn 足够
        if (code.getHttpStatus() >= 500) {
            log.error("接口业务异常: code={}, message={}", code.getCode(), e.getMessage(), e);
        } else {
            log.warn("接口业务异常: code={}, message={}", code.getCode(), e.getMessage());
        }
        return build(code, e.getMessage());
    }

    /**
     * {@code @Valid @RequestBody} 校验失败。
     * 把所有字段错误拼成一条 message 返回，前端可以直接显示在表单顶部；
     * 只返回第一条会让用户改一个提交一次，体验很差。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(this::describe)
                .collect(Collectors.joining("；"));
        log.warn("接口参数校验失败: {}", detail);
        return build(ErrorCode.PARAM_INVALID, detail);
    }

    /** 方法参数上的约束（如 @RequestParam + @Min）校验失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraint(ConstraintViolationException e) {
        String detail = e.getConstraintViolations().stream()
                .map(this::describe)
                .collect(Collectors.joining("；"));
        log.warn("接口参数校验失败: {}", detail);
        return build(ErrorCode.PARAM_INVALID, detail);
    }

    /**
     * 请求体不是合法 JSON，或字段类型对不上（例如 priority 传了 "高"）。
     * 这类错误在进入方法之前就抛了，@Valid 根本轮不到，必须单独接。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleUnreadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return build(ErrorCode.PARAM_INVALID, "请求体不是合法的 JSON，或字段类型与接口定义不符");
    }

    /** 路径变量 / 查询参数类型转换失败，例如 /api/v1/tasks/abc */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String detail = "参数 " + e.getName() + " 的值 '" + e.getValue() + "' 类型不合法";
        log.warn("接口参数类型错误: {}", detail);
        return build(ErrorCode.PARAM_INVALID, detail);
    }

    /** 必填查询参数缺失 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        return build(ErrorCode.PARAM_INVALID, "缺少必填参数 " + e.getParameterName());
    }

    /** Content-Type 不是 application/json（常见于前端忘了设 header） */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMediaType(HttpMediaTypeNotSupportedException e) {
        return build(ErrorCode.PARAM_INVALID,
                "Content-Type 应为 application/json，实际为 " + e.getContentType());
    }

    /**
     * 数据库不可用：连接池拿不到连接、SQL 语法错误、超时等，Spring 统一包装成 DataAccessException。
     * 必须单独接 —— 否则会被下面的 Exception 兜底成 50001，运维看到"系统繁忙"根本不知道是库挂了。
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Result<Void>> handleDataAccess(DataAccessException e) {
        log.error("数据访问异常", e);
        return build(ErrorCode.DATA_ACCESS_ERROR, null);
    }

    /** 兜底：任何未预期的异常都不把堆栈返回给客户端 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpected(Exception e) {
        log.error("接口未预期异常", e);
        return build(ErrorCode.INTERNAL_ERROR, null);
    }

    private ResponseEntity<Result<Void>> build(ErrorCode code, String detail) {
        return ResponseEntity.status(code.getHttpStatus()).body(Result.<Void>fail(code, detail));
    }

    /** 字段名 + 中文提示，例如 "title: 任务标题不能为空" */
    private String describe(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private String describe(ConstraintViolation<?> violation) {
        return violation.getPropertyPath() + ": " + violation.getMessage();
    }
}
