package cn.edu.mju.band.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.assertj.core.api.Assertions.assertThat;

/** 视图层格式化工具的边界值测试：覆盖 null、合法枚举值、越界值三条分支。 */
@DisplayName("TaskFormatter 单元测试")
class TaskFormatterTest {

    @ParameterizedTest
    @CsvSource({"1, 低", "2, 中", "3, 高", "9, 9"})
    @DisplayName("优先级映射：1/2/3 与越界值")
    void shouldMapPriority(byte priority, String expected) {
        assertThat(TaskFormatter.getPriorityText(priority)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"0, 待办", "1, 进行中", "2, 已完成", "9, 9"})
    @DisplayName("状态映射：0/1/2 与越界值")
    void shouldMapStatus(byte status, String expected) {
        assertThat(TaskFormatter.getStatusText(status)).isEqualTo(expected);
    }

    @Test
    @DisplayName("null 入参返回空串，避免模板拆箱 NPE")
    void shouldReturnEmptyForNull() {
        assertThat(TaskFormatter.getPriorityText(null)).isEmpty();
        assertThat(TaskFormatter.getStatusText(null)).isEmpty();
        assertThat(TaskFormatter.formatDate(null)).isEmpty();
    }

    @Test
    @DisplayName("日期统一格式化为 yyyy-MM-dd")
    void shouldFormatDate() throws ParseException {
        java.util.Date date = new SimpleDateFormat("yyyy-MM-dd").parse("2026-11-04");
        assertThat(TaskFormatter.formatDate(date)).isEqualTo("2026-11-04");
    }
}
