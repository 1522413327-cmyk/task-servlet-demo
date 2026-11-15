package cn.edu.mju.band.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 统一响应体（实验环节二）。所有 {@code /api/**} 接口，无论成功失败，返回的 JSON 结构完全一致：
 *
 * <pre>
 * {
 *   "code": 0,
 *   "message": "ok",
 *   "data": { ... },
 *   "timestamp": "2026-11-15 21:30:00"
 * }
 * </pre>
 *
 * <p><b>为什么必须统一</b>：如果成功时返回裸对象、失败时返回 {@code {"error": "..."}}，
 * 前端每写一个接口都要判断"这次是什么形状"，异常分支会散落在每个页面里。
 * 统一之后前端只写一次拦截器：{@code code != 0} 就弹 message。</p>
 *
 * <p><b>为什么 data 失败时为 null 而不是省略</b>：字段恒定存在，前端取值不用判 key 是否存在，
 * 只需判 null。省略字段会让 TypeScript 的类型声明和运行时对不上。</p>
 *
 * @param <T> 业务数据类型；无返回值的接口用 {@code Result<Void>}
 */
@Schema(description = "统一响应体")
public class Result<T> {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 业务错误码，0 表示成功，见 {@link ErrorCode} */
    @Schema(description = "业务错误码，0 表示成功", example = "0")
    private final int code;

    /** 提示信息，成功固定为 ok，失败为可直接展示给用户的中文文案 */
    @Schema(description = "提示信息", example = "ok")
    private final String message;

    /** 业务数据，失败时为 null */
    @Schema(description = "业务数据，失败时为 null")
    private final T data;

    /** 服务端响应时间，便于排查"前端说没收到"这类问题时对齐时间线 */
    @Schema(description = "响应时间 yyyy-MM-dd HH:mm:ss", example = "2026-11-15 21:30:00")
    private final String timestamp;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now().format(TS_FMT);
    }

    /** 成功并携带数据 */
    public static <T> Result<T> ok(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    /** 成功但无数据（删除接口） */
    public static <T> Result<T> ok() {
        return ok(null);
    }

    /** 失败，使用错误码自带的标准文案 */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 失败，并用更具体的信息覆盖标准文案。
     * 例如 40001 的标准文案是"请求参数不合法"，覆盖成"title: 任务标题不能为空"才有排查价值。
     * 覆盖的只是 message，code 不变 —— 前端的分支判断永远依赖 code。
     */
    public static <T> Result<T> fail(ErrorCode errorCode, String detail) {
        String message = (detail == null || detail.isBlank()) ? errorCode.getMessage() : detail;
        return new Result<>(errorCode.getCode(), message, null);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
