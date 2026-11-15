package cn.edu.mju.band.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 任务新增 / 更新入参。
 *
 * <p><b>为什么不直接用 {@code Task} 接收 @RequestBody</b>：实体的 createDate 由数据库生成、
 * id 由路径参数给出，如果让客户端能在 body 里传这些字段，就是一个越权写入的入口
 * （客户端可以伪造创建时间）。入参对象只保留"允许客户端决定的字段"，边界即白名单。</p>
 *
 * <p><b>PUT 是全量更新</b>：除 dueDate 外所有字段必填；dueDate 传 null 表示清空
 * （对应实体上的 {@code updateStrategy = ALWAYS}）。只想改状态请用
 * {@code PATCH /api/v1/tasks/{id}/status}。</p>
 */
@Schema(description = "任务新增/更新请求")
public class TaskRequest {

    @NotBlank(message = "任务标题不能为空")
    @Size(max = 50, message = "任务标题不能超过 50 个字符")
    @Schema(description = "任务标题，1~50 字符，首尾空格会被去除",
            example = "完成 DevOps 实验报告", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotNull(message = "请选择优先级")
    @Min(value = 1, message = "优先级只能是 1(低)、2(中)、3(高)")
    @Max(value = 3, message = "优先级只能是 1(低)、2(中)、3(高)")
    @Schema(description = "优先级：1=低 2=中 3=高", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Byte priority;

    @NotNull(message = "请选择状态")
    @Min(value = 0, message = "状态只能是 0(待办)、1(进行中)、2(已完成)")
    @Max(value = 2, message = "状态只能是 0(待办)、1(进行中)、2(已完成)")
    @Schema(description = "状态：0=待办 1=进行中 2=已完成", example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    private Byte status;

    /**
     * 允许为空字符串或 null（表示不设置截止日期）。
     * 正则只保证"长得像日期"，像 2026-02-30 这种不存在的日期会在转换成 LocalDate 时
     * 抛异常并返回 40002 —— 正则做不到日历级别的校验。
     */
    @Pattern(regexp = "^$|\\d{4}-\\d{2}-\\d{2}", message = "截止日期格式应为 yyyy-MM-dd")
    @Schema(description = "截止日期 yyyy-MM-dd；null 或空串表示不设置/清空",
            example = "2026-11-30", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String dueDate;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Byte getPriority() {
        return priority;
    }

    public void setPriority(Byte priority) {
        this.priority = priority;
    }

    public Byte getStatus() {
        return status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }
}
