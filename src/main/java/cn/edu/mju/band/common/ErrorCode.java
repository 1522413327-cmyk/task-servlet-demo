package cn.edu.mju.band.common;

/**
 * 业务错误码（实验环节二：接口手册中的“错误码表”由本枚举唯一维护）。
 *
 * <p><b>为什么不用裸数字 500</b>：HTTP 状态码只能表达"哪一类错"，
 * 表达不了"到底哪条业务规则没过"。前端要根据错误码分支处理，
 * 所以约定：HTTP 状态码给传输层看，{@code code} 给业务层看，两者一一对应。</p>
 *
 * <p><b>编码规则</b>：5 位十进制，前 3 位对齐 HTTP 状态码，后 2 位是该类错误的序号。</p>
 * <pre>
 *   0      成功（不是 200 —— code 表达的是业务结果，成功用 0 更不容易和 HTTP 混淆）
 *   400xx  客户端参数问题，改请求即可解决
 *   404xx  资源不存在
 *   500xx  服务端问题，重试或联系维护者
 * </pre>
 *
 * <p>注：<b>405（方法不支持）没有对应错误码</b>。该异常在请求还没匹配到任何 Controller 方法时
 * 就已抛出，此时 {@code @RestControllerAdvice} 拿不到 handler 类型，无法参与处理，
 * 响应体是 Spring MVC 的默认结构。这类"框架层错误"不纳入本项目的统一错误码体系。</p>
 *
 * <p>错误码一旦发布就<b>不能再改含义</b>，只能新增：前端会把数字写进 if 分支里。</p>
 */
public enum ErrorCode {

    /** 成功。data 字段携带业务数据，message 固定为 ok */
    SUCCESS(0, "ok", 200),

    /** 参数校验失败：字段缺失、长度超限、枚举值越界、类型转换失败等 */
    PARAM_INVALID(40001, "请求参数不合法", 400),

    /** 日期能被正则匹配但不是真实日期，例如 2026-02-30 */
    DATE_FORMAT_INVALID(40002, "日期格式错误，正确格式为 yyyy-MM-dd，且必须是真实存在的日期", 400),

    /** 按 id 查询 / 更新 / 删除时目标不存在 */
    TASK_NOT_FOUND(40401, "任务不存在", 404),

    /** 未登录或会话过期。页面请求会被重定向到登录页，接口请求返回此错误码 */
    UNAUTHORIZED(40101, "未登录或会话已过期，请先登录", 401),

    /** 兜底：未被更具体处理器捕获的异常 */
    INTERNAL_ERROR(50001, "系统繁忙，请稍后重试", 500),

    /** 数据库不可用、SQL 执行失败、连接池耗尽 */
    DATA_ACCESS_ERROR(50002, "数据访问失败，请稍后重试", 503),

    /** 写操作影响行数为 0（例如记录在检查后被并发删除） */
    WRITE_FAILED(50003, "数据写入失败", 500);

    private final int code;
    private final String message;
    private final int httpStatus;

    ErrorCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    /** 与该业务错误码配套的 HTTP 状态码，由 ApiExceptionHandler 写入响应 */
    public int getHttpStatus() {
        return httpStatus;
    }
}
