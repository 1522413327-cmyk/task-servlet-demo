package cn.edu.mju.band.form;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 环节四：直接验证 TaskForm 上的 jakarta.validation 约束，
 * 不依赖 Spring 容器，覆盖等价类与边界值（空白、超长、null、非法日期）。
 */
@DisplayName("TaskForm 服务端校验规则")
class TaskFormValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeFactory() {
        factory.close();
    }

    private TaskForm valid() {
        TaskForm f = new TaskForm();
        f.setOpt("add");
        f.setTitle("写实验报告");
        f.setPriority((byte) 2);
        f.setStatus((byte) 1);
        f.setDueDate("2026-11-20");
        return f;
    }

    private Set<String> messages(TaskForm f) {
        return validator.validate(f).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("合法表单零违规")
    void validFormHasNoViolations() {
        assertThat(messages(valid())).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("标题为 null / 空 / 全空白时触发 @NotBlank")
    void blankTitleShouldBeRejected(String title) {
        TaskForm f = valid();
        f.setTitle(title);
        assertThat(messages(f)).contains("任务标题不能为空");
    }

    @Test
    @DisplayName("标题 51 个字符触发 @Size 上限，50 个字符恰好合法")
    void tooLongTitleShouldBeRejected() {
        TaskForm tooLong = valid();
        tooLong.setTitle("a".repeat(51));
        assertThat(messages(tooLong)).contains("任务标题不能超过 50 个字符");

        TaskForm boundary = valid();
        boundary.setTitle("a".repeat(50));
        assertThat(messages(boundary)).isEmpty();
    }

    @Test
    @DisplayName("优先级为 null 触发 @NotNull")
    void nullPriorityShouldBeRejected() {
        TaskForm f = valid();
        f.setPriority(null);
        assertThat(messages(f)).contains("请选择优先级");
    }

    @Test
    @DisplayName("状态为 null 触发 @NotNull")
    void nullStatusShouldBeRejected() {
        TaskForm f = valid();
        f.setStatus(null);
        assertThat(messages(f)).contains("请选择状态");
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-date", "2026/11/04", "11-04", "2026-1-1"})
    @DisplayName("非法日期格式触发 @Pattern 并回显中文提示")
    void invalidDateShouldBeRejected(String date) {
        TaskForm f = valid();
        f.setDueDate(date);
        assertThat(messages(f)).contains("截止日期格式应为 yyyy-MM-dd");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"2026-11-20", "2026-01-01"})
    @DisplayName("空日期与合法 yyyy-MM-dd 均放行")
    void emptyOrValidDateShouldPass(String date) {
        TaskForm f = valid();
        f.setDueDate(date);
        assertThat(messages(f)).isEmpty();
    }
}
