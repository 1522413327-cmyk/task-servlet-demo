package cn.edu.mju.band.api;

import cn.edu.mju.band.api.dto.TaskStatusRequest;
import cn.edu.mju.band.common.ErrorCode;
import cn.edu.mju.band.common.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 异常处理器单元测试：验证"每种异常都被翻译成正确的 HTTP 状态 + 业务码"。
 *
 * <p>契约测试走不到的分支在这里补：参数缺失、Content-Type 不对、
 * 方法级校验违规、请求体解析失败。这些分支平时不出现，
 * 一出现就是给调用方看的门面，必须逐个确认。</p>
 */
@DisplayName("接口异常处理器单元测试")
class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    private void assertCode(ResponseEntity<Result<Void>> response, int httpStatus, int bizCode) {
        assertThat(response.getStatusCode().value()).isEqualTo(httpStatus);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(bizCode);
        assertThat(response.getBody().getData()).isNull();
    }

    @Test
    @DisplayName("缺少必填参数 -> 400 / 40001，message 带参数名")
    void missingParamShouldReturn40001() {
        ResponseEntity<Result<Void>> r = handler.handleMissingParam(
                new MissingServletRequestParameterException("title", "String"));

        assertCode(r, 400, ErrorCode.PARAM_INVALID.getCode());
        assertThat(r.getBody().getMessage()).contains("title");
    }

    @Test
    @DisplayName("Content-Type 不是 JSON -> 400 / 40001，message 指出实际类型")
    void wrongMediaTypeShouldReturn40001() {
        ResponseEntity<Result<Void>> r = handler.handleMediaType(
                new HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, java.util.List.of()));

        assertCode(r, 400, ErrorCode.PARAM_INVALID.getCode());
        assertThat(r.getBody().getMessage()).contains("application/json");
    }

    @Test
    @DisplayName("请求体解析失败 -> 400 / 40001，不把 Jackson 堆栈透给客户端")
    void unreadableBodyShouldReturn40001() {
        ResponseEntity<Result<Void>> r = handler.handleUnreadable(
                new HttpMessageNotReadableException("JSON parse error", new MockHttpInputMessage(new byte[0])));

        assertCode(r, 400, ErrorCode.PARAM_INVALID.getCode());
        assertThat(r.getBody().getMessage()).doesNotContain("Jackson").doesNotContain("Exception");
    }

    @Test
    @DisplayName("方法级约束违规 -> 400 / 40001，message 为 字段: 提示")
    void constraintViolationShouldReturn40001() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<TaskStatusRequest>> violations = validator.validate(new TaskStatusRequest());
        assertThat(violations).isNotEmpty();     // status 为 null，@NotNull 必然命中

        ResponseEntity<Result<Void>> r = handler.handleConstraint(new ConstraintViolationException(violations));

        assertCode(r, 400, ErrorCode.PARAM_INVALID.getCode());
        assertThat(r.getBody().getMessage()).contains("请选择状态");
    }

    @Test
    @DisplayName("路径参数类型错误 -> 400 / 40001，message 带参数名与非法值")
    void typeMismatchShouldReturn40001() {
        ResponseEntity<Result<Void>> r = handler.handleTypeMismatch(
                new MethodArgumentTypeMismatchException("abc", Integer.class, "id", null, null));

        assertCode(r, 400, ErrorCode.PARAM_INVALID.getCode());
        assertThat(r.getBody().getMessage()).contains("id").contains("abc");
    }
}
