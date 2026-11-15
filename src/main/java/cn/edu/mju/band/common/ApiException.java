package cn.edu.mju.band.common;

/**
 * 业务异常：在 Service / Controller 里主动抛出，由
 * {@link cn.edu.mju.band.api.ApiExceptionHandler} 统一翻译成响应体。
 *
 * <p><b>为什么不直接 throw RuntimeException</b>：调用方拿到异常时只知道"失败了"，
 * 不知道"为什么失败、该返回什么错误码"。把 {@link ErrorCode} 挂在异常上，
 * 错误码与文案就跟着异常一路传到出口，中间层不需要 if 判断类型。</p>
 *
 * <p><b>为什么不 return false / null 表示失败</b>：那会让每个调用点都写
 * "如果失败就……"，漏判一处就是静默的错误结果。异常会把失败强制暴露出来。</p>
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;

    /** 使用错误码自带的标准文案 */
    public ApiException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 使用更具体的文案（code 不变），例如"id=999 的任务不存在" */
    public ApiException(ErrorCode errorCode, String detail) {
        super(detail == null || detail.isBlank() ? errorCode.getMessage() : detail);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
