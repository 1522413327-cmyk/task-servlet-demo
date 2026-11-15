package cn.edu.mju.band.buss;

import cn.edu.mju.band.buss.impl.TaskBuss;
import cn.edu.mju.band.entity.PageResult;
import cn.edu.mju.band.entity.Task;
import cn.edu.mju.band.mapper.TaskMapper;
import cn.edu.mju.band.support.MybatisPlusTestSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 环节三 / 四：TaskBuss 的 JUnit 5 + Mockito 单元测试。
 * TaskMapper 全程是 @Mock，测试用例不启动 Spring、不连 MySQL、不依赖网络，
 * 每个用例互相独立（@BeforeEach 重建数据、MockitoExtension 重建 mock）。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskBuss 单元测试（持久层完全隔离）")
class TaskBussTest {

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskBuss taskBuss;

    @Captor
    private ArgumentCaptor<Page<Task>> pageCaptor;

    @Captor
    private ArgumentCaptor<LambdaQueryWrapper<Task>> wrapperCaptor;

    private Task t1;
    private Task t2;

    @BeforeAll
    static void initMetadata() {
        MybatisPlusTestSupport.initTableInfo();
    }

    @BeforeEach
    void setUp() {
        t1 = new Task();
        t1.setId(1);
        t1.setTitle("写实验报告");
        t1.setStatus((byte) 1);
        t2 = new Task();
        t2.setId(2);
        t2.setTitle("联调分页");
        t2.setStatus((byte) 2);
    }

    /** 构造 MP 分页返回值：getPages() 由 total / size 自动算，无需手工 setPages */
    private Page<Task> page(long current, long size, long total, List<Task> records) {
        Page<Task> p = new Page<>(current, size);
        p.setTotal(total);
        p.setRecords(records);
        return p;
    }

    // ==================== 正常流 ====================

    @Test
    @DisplayName("list(): 委托 Mapper 查询并按 id 倒序返回")
    void listShouldDelegateToMapper() {
        when(taskMapper.selectList(any())).thenReturn(Arrays.asList(t1, t2));

        List<Task> result = taskBuss.list();

        assertThat(result).containsExactly(t1, t2);
        verify(taskMapper, times(1)).selectList(any());
    }

    @Test
    @DisplayName("add(): 影响 1 行返回 true")
    void addShouldReturnTrueWhenInsertSucceeds() {
        when(taskMapper.insert(t1)).thenReturn(1);
        assertThat(taskBuss.add(t1)).isTrue();
        verify(taskMapper).insert(t1);
    }

    @Test
    @DisplayName("add(): 影响 0 行返回 false")
    void addShouldReturnFalseWhenInsertAffectsZeroRows() {
        when(taskMapper.insert(t1)).thenReturn(0);
        assertThat(taskBuss.add(t1)).isFalse();
    }

    @Test
    @DisplayName("delete()/findById(): 正常 id 透传给 Mapper")
    void normalIdShouldBePassedThrough() {
        when(taskMapper.deleteById(7)).thenReturn(1);
        when(taskMapper.selectById(7)).thenReturn(t1);

        assertThat(taskBuss.delete(7)).isTrue();
        assertThat(taskBuss.findById(7)).isSameAs(t1);
    }

    @Test
    @DisplayName("search(): 正常分页，五要素与上下页标志正确")
    void searchShouldReturnExpectedPageResult() {
        doReturn(page(1, 5, 12, Arrays.asList(t1, t2)))
                .when(taskMapper).selectPage(any(), any());

        PageResult<Task> result = taskBuss.search("报告", (byte) 1, 1, 5);

        assertThat(result.getList()).containsExactly(t1, t2);
        assertThat(result.getPageNo()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(5);
        assertThat(result.getTotalCount()).isEqualTo(12);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.isHasPrev()).isFalse();
        assertThat(result.isHasNext()).isTrue();
        verify(taskMapper, times(1)).selectPage(any(), any());
    }

    @Test
    @DisplayName("search(): 页码超出总页数时回到最后一页重查")
    void searchShouldFallbackToLastPageWhenOverflow() {
        // total=6, size=5 -> 共 2 页；请求第 9 页，应重查第 2 页
        doReturn(page(9, 5, 6, Arrays.asList(t1)),
                 page(2, 5, 6, Arrays.asList(t2)))
                .when(taskMapper).selectPage(any(), any());

        PageResult<Task> result = taskBuss.search(null, null, 9, 5);

        assertThat(result.getPageNo()).isEqualTo(2);
        assertThat(result.getList()).containsExactly(t2);
        verify(taskMapper, times(2)).selectPage(any(), any());
    }

    @Test
    @DisplayName("search(): 条件正确传入 Wrapper，标题已 trim")
    void searchShouldBuildExpectedWrapper() {
        doReturn(page(1, 5, 1, Arrays.asList(t1)))
                .when(taskMapper).selectPage(any(), any());

        taskBuss.search("  报告  ", (byte) 2, 1, 5);

        verify(taskMapper).selectPage(pageCaptor.capture(), wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getCustomSqlSegment()).contains("LIKE");
        // MP 的 like 会自动把参数包成 %xxx%，因此值本身应是 trim 后的「报告」
        assertThat(wrapperCaptor.getValue().getParamNameValuePairs().toString())
                .contains("%报告%");
    }

    // ==================== 边界值 / 参数化 ====================

    @ParameterizedTest
    @CsvSource({"0, 0", "-3, -5", "101, 1", "1000, 1"})
    @DisplayName("search(): 非法 pageSize / pageNo 兜底为 5 / 1")
    void searchShouldClampIllegalPagination(int pageSize, int pageNo) {
        doReturn(page(1, 5, 0, Arrays.asList()))
                .when(taskMapper).selectPage(any(), any());

        taskBuss.search(null, null, pageNo, pageSize);

        verify(taskMapper).selectPage(pageCaptor.capture(), any());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(5);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t"})
    @DisplayName("search(): 空白标题视为不限，不拼 LIKE 条件")
    void searchShouldDropBlankTitle(String blankTitle) {
        doReturn(page(1, 5, 0, Arrays.asList()))
                .when(taskMapper).selectPage(any(), any());

        taskBuss.search(blankTitle, null, 1, 5);

        verify(taskMapper).selectPage(any(), wrapperCaptor.capture());
        assertThat(wrapperCaptor.getValue().getCustomSqlSegment()).doesNotContain("LIKE");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -999})
    @DisplayName("findById(): 非正数 id 直接返回 null，不打扰数据库")
    void findByIdShouldRejectNonPositiveId(int id) {
        assertThat(taskBuss.findById(id)).isNull();
        verifyNoInteractions(taskMapper);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -999})
    @DisplayName("delete(): 非正数 id 直接返回 false，不打扰数据库")
    void deleteShouldRejectNonPositiveId(int id) {
        assertThat(taskBuss.delete(id)).isFalse();
        verifyNoInteractions(taskMapper);
    }

    @Test
    @DisplayName("add()/update(): null 实体或缺失主键被边界校验拦下")
    void nullEntitiesShouldBeRejectedBeforeHittingMapper() {
        assertThat(taskBuss.add(null)).isFalse();
        assertThat(taskBuss.update(null)).isFalse();
        assertThat(taskBuss.update(new Task())).isFalse();   // id == null
        verifyNoInteractions(taskMapper);
    }

    @Test
    @DisplayName("业务参数随 profile 变化：非法 pageSize 回落到当前环境的默认值")
    void searchShouldUseConfiguredDefaultPageSize() {
        // 模拟 application-test.yml 的 app.task.default-page-size: 3
        taskBuss.applyPaginationConfig(3, 20);
        doReturn(page(1, 3, 0, Arrays.asList()))
                .when(taskMapper).selectPage(any(), any());

        taskBuss.search(null, null, 1, 999);

        verify(taskMapper).selectPage(pageCaptor.capture(), any());
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(3);
    }

    // ==================== 异常流（环节四：数据库宕机） ====================

    @Test
    @DisplayName("search(): 数据库宕机时降级为空结果，不抛异常、不重复查库")
    void searchShouldDegradeGracefullyWhenDatabaseDown() {
        doThrow(new RuntimeException("Connection refused"))
                .when(taskMapper).selectPage(any(), any());

        PageResult<Task> result = taskBuss.search("报告", null, 2, 5);

        assertThat(result.getList()).isEmpty();
        assertThat(result.getTotalCount()).isZero();
        assertThat(result.getTotalPages()).isZero();
        assertThat(result.getPageNo()).isEqualTo(1);
        assertThat(result.isHasNext()).isFalse();
        verify(taskMapper, times(1)).selectPage(any(), any());
    }

    @Test
    @DisplayName("写操作不降级：Mapper 异常原样上抛，便于调用方感知失败")
    void writeOperationsShouldPropagateMapperException() {
        when(taskMapper.insert(t1)).thenThrow(new RuntimeException("Connection refused"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> taskBuss.add(t1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Connection refused");
    }
}
