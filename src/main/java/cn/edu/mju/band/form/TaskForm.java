package cn.edu.mju.band.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 任务表单对象（实验环节四：Spring Validation 服务端校验）。
 *
 * <p><b>为什么不直接把 @ModelAttribute 绑到 Task 实体</b>：表单里的 dueDate 是
 * "yyyy-MM-dd" 字符串，而实体字段是 {@code java.util.Date}。直接绑实体时，
 * 一旦日期格式非法，Spring 转换失败会把用户输入丢掉，错误回显里日期框只能显示空白。
 * 用 String 接收则可以让 {@code th:field} 原样回显用户填的内容，
 * 校验通过后再由 Controller 转成 Date，兼顾体验与类型安全。</p>
 *
 * <p>约束注解都带中文 message，模板用 {@code th:errors} 直接显示，
 * 不需要在 Controller 里手工拼错误文案。</p>
 */
@Data
public class TaskForm {

    /** 编辑时的主键，新增时为空 */
    private Integer id;

    /** 隐藏域：add / update，决定走哪个业务方法 */
    private String opt;

    @NotBlank(message = "任务标题不能为空")
    @Size(max = 50, message = "任务标题不能超过 50 个字符")
    private String title;

    @NotNull(message = "请选择优先级")
    private Byte priority;

    @NotNull(message = "请选择状态")
    private Byte status;

    /** 允许为空；非空时必须是 yyyy-MM-dd，格式错误回显而不是被静默丢弃 */
    @Pattern(regexp = "^$|\\d{4}-\\d{2}-\\d{2}", message = "截止日期格式应为 yyyy-MM-dd")
    private String dueDate;
}
