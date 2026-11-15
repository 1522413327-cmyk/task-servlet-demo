package cn.edu.mju.band.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 状态流转入参（PATCH /api/v1/tasks/{id}/status）。
 *
 * <p>单独建一个只有 status 字段的 DTO，而不是复用 {@link TaskRequest}：
 * 复用会让"只改状态"的请求也不得不传 title/priority，前端容易漏传导致校验失败，
 * 也给了它顺手改标题的机会。接口的入参应该恰好是这次操作能改的东西。</p>
 */
@Schema(description = "任务状态流转请求")
public class TaskStatusRequest {

    @NotNull(message = "请选择状态")
    @Min(value = 0, message = "状态只能是 0(待办)、1(进行中)、2(已完成)")
    @Max(value = 2, message = "状态只能是 0(待办)、1(进行中)、2(已完成)")
    @Schema(description = "目标状态：0=待办 1=进行中 2=已完成", example = "2",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Byte status;

    public Byte getStatus() {
        return status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }
}
