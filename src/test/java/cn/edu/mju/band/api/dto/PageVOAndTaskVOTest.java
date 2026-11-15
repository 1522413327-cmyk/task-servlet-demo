package cn.edu.mju.band.api.dto;

import cn.edu.mju.band.entity.Task;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 出参对象单元测试：分页计算与日期格式化。
 *
 * <p>日期格式化是接口最容易出错的地方：JDBC 从 MySQL 的 DATE / DATETIME 列读出来的是
 * {@code java.sql.Date} / {@code java.sql.Timestamp}，它们都继承了 java.util.Date，
 * 但 java.sql.Date 覆写了 {@code toInstant()} 直接抛异常。这里用真实子类做输入，
 * 保证 TaskVO 走的是毫秒值换算而不是 toInstant()。</p>
 */
@DisplayName("出参对象单元测试")
class PageVOAndTaskVOTest {

    // ==================== PageVO ====================

    @Test
    @DisplayName("总页数向上取整：30 条 / 每页 5 条 = 6 页")
    void totalPagesShouldRoundUp() {
        assertThat(new PageVO<>(List.of(), 1, 5, 30).getTotalPages()).isEqualTo(6);
    }

    @Test
    @DisplayName("总条数不能整除时多算一页：31 条 / 每页 5 条 = 7 页")
    void totalPagesShouldRoundUpWhenRemainder() {
        assertThat(new PageVO<>(List.of(), 1, 5, 31).getTotalPages()).isEqualTo(7);
    }

    @Test
    @DisplayName("pageSize 非法时总页数为 0，不能抛除零异常")
    void zeroPageSizeShouldNotDivideByZero() {
        assertThat(new PageVO<>(List.of(), 1, 0, 30).getTotalPages()).isZero();
    }

    @Test
    @DisplayName("list 传 null 时归一为空集合，前端不必判 null")
    void nullListShouldBecomeEmpty() {
        PageVO<String> page = new PageVO<>(null, 1, 5, 0);
        assertThat(page.getList()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("setter 可用于反序列化/构造，字段原样读出")
    void accessorsShouldWork() {
        PageVO<String> page = new PageVO<>();
        page.setList(List.of("a"));
        page.setPageNo(2);
        page.setPageSize(3);
        page.setTotalCount(4);
        page.setTotalPages(2);

        assertThat(page.getList()).containsExactly("a");
        assertThat(page.getPageNo()).isEqualTo(2);
        assertThat(page.getPageSize()).isEqualTo(3);
        assertThat(page.getTotalCount()).isEqualTo(4);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    // ==================== TaskVO ====================

    private Task task() {
        Task t = new Task();
        t.setId(7);
        t.setTitle("联调接口");
        t.setStatus((byte) 1);
        t.setPriority((byte) 3);
        t.setDueDate(Date.valueOf("2026-11-30"));                      // java.sql.Date
        t.setCreateDate(Timestamp.valueOf("2026-11-15 21:30:00"));     // java.sql.Timestamp
        return t;
    }

    @Test
    @DisplayName("实体转 VO：日期格式化为字符串，状态/优先级翻译成中文")
    void fromShouldFormatAndTranslate() {
        TaskVO vo = TaskVO.from(task());

        assertThat(vo.getId()).isEqualTo(7);
        assertThat(vo.getTitle()).isEqualTo("联调接口");
        assertThat(vo.getStatus()).isEqualTo((byte) 1);
        assertThat(vo.getStatusText()).isEqualTo("进行中");
        assertThat(vo.getPriority()).isEqualTo((byte) 3);
        assertThat(vo.getPriorityText()).isEqualTo("高");
        assertThat(vo.getDueDate()).isEqualTo("2026-11-30");
        assertThat(vo.getCreateDate()).isEqualTo("2026-11-15 21:30:00");
    }

    @Test
    @DisplayName("日期为 null 时输出空字符串，不是 null")
    void nullDatesShouldBecomeEmptyString() {
        Task t = task();
        t.setDueDate(null);
        t.setCreateDate(null);

        TaskVO vo = TaskVO.from(t);
        assertThat(vo.getDueDate()).isEmpty();
        assertThat(vo.getCreateDate()).isEmpty();
    }

    @Test
    @DisplayName("传入 null 实体返回 null，而不是抛 NPE")
    void fromNullShouldReturnNull() {
        assertThat(TaskVO.from(null)).isNull();
    }
}
