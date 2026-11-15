package cn.edu.mju.band.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/** PageResult 的边界值与分页算法测试（纯 POJO，无需任何 Mock）。 */
@DisplayName("PageResult 分页载体单元测试")
class PageResultTest {

    @ParameterizedTest
    @CsvSource({
            "0,  5, 0",
            "1,  5, 1",
            "5,  5, 1",
            "6,  5, 2",
            "11, 5, 3",
            "100, 10, 10"
    })
    @DisplayName("总页数按向上取整计算")
    void shouldComputeTotalPages(int totalCount, int pageSize, int expectedPages) {
        PageResult<String> result =
                new PageResult<>(Arrays.asList("a"), 1, pageSize, totalCount);
        assertThat(result.getTotalPages()).isEqualTo(expectedPages);
    }

    @Test
    @DisplayName("pageSize <= 0 时总页数为 0，不触发除零")
    void shouldHandleNonPositivePageSize() {
        assertThat(new PageResult<>(null, 1, 0, 10).getTotalPages()).isZero();
        assertThat(new PageResult<>(null, 1, -5, 10).getTotalPages()).isZero();
    }

    @Test
    @DisplayName("传入 null 列表被转换成空集合，页面遍历不会 NPE")
    void shouldConvertNullListToEmpty() {
        PageResult<Task> result = new PageResult<>(null, 1, 5, 0);
        assertThat(result.getList()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("首页 / 中间页 / 末页的上下页标志正确")
    void shouldExposePrevNextFlags() {
        PageResult<Task> first = new PageResult<>(null, 1, 5, 11);
        assertThat(first.isHasPrev()).isFalse();
        assertThat(first.isHasNext()).isTrue();

        PageResult<Task> middle = new PageResult<>(null, 2, 5, 11);
        assertThat(middle.isHasPrev()).isTrue();
        assertThat(middle.isHasNext()).isTrue();

        PageResult<Task> last = new PageResult<>(null, 3, 5, 11);
        assertThat(last.isHasPrev()).isTrue();
        assertThat(last.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("无参构造提供安全默认值")
    void defaultConstructorShouldHaveSafeDefaults() {
        PageResult<Task> result = new PageResult<>();
        assertThat(result.getList()).isEmpty();
        assertThat(result.getPageNo()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("所有 setter 与 getter 读写一致，标志随之更新")
    void settersShouldRoundTrip() {
        PageResult<Task> result = new PageResult<>();
        result.setList(Arrays.asList(new Task()));
        result.setPageNo(3);
        result.setPageSize(10);
        result.setTotalCount(21);
        result.setTotalPages(3);

        assertThat(result.getList()).hasSize(1);
        assertThat(result.getPageNo()).isEqualTo(3);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getTotalCount()).isEqualTo(21);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.isHasPrev()).isTrue();
        assertThat(result.isHasNext()).isFalse();
    }
}
