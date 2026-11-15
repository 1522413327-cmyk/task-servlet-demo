package cn.edu.mju.band.api;

import cn.edu.mju.band.buss.ITaskBuss;
import cn.edu.mju.band.entity.PageResult;
import cn.edu.mju.band.entity.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.sql.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * REST 接口的契约测试（实验环节二）。
 *
 * <p>用 {@code standaloneSetup} 只装载 ApiTaskController + ApiExceptionHandler：
 * 不启动 Spring 容器、不需要 MySQL，因此能和其他 109 个纯单元测试一样进 CI。
 * 这正是"接口契约"该被测的地方 —— 断言的是 JSON 的形状与错误码，
 * 而不是业务层怎么算（那由 TaskBussTest 负责）。</p>
 *
 * <p>覆盖的场景：6 个接口的成功路径 + 参数校验/范围校验 + 资源不存在 + 写失败 +
 * 数据层异常 + 未预期异常，共 21 条。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("任务 REST 接口契约测试")
class ApiTaskControllerTest {

    private static final String BASE = "/api/v1/tasks";

    @Mock
    private ITaskBuss buss;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // 默认每页 5 条，与 dev 环境一致
        ApiTaskController controller = new ApiTaskController(buss, 5);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    private Task task(int id, String title, byte status, byte priority, String dueDate) {
        Task t = new Task();
        t.setId(id);
        t.setTitle(title);
        t.setStatus(status);
        t.setPriority(priority);
        t.setDueDate(dueDate == null ? null : Date.valueOf(dueDate));
        t.setCreateDate(new java.util.Date(1794000000000L));  // 固定时间戳，便于断言
        return t;
    }

    // ==================== 查询列表 ====================

    @Test
    @DisplayName("GET 列表：返回分页结构与状态/优先级中文名")
    void listShouldReturnPage() throws Exception {
        PageResult<Task> page = new PageResult<>(List.of(task(2, "写实验报告", (byte) 0, (byte) 2, "2026-11-30")),
                1, 5, 12);
        when(buss.search(any(), any(), eq(1), eq(5))).thenReturn(page);

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.totalCount").value(12))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.pageNo").value(1))
                .andExpect(jsonPath("$.data.list[0].title").value("写实验报告"))
                .andExpect(jsonPath("$.data.list[0].statusText").value("待办"))
                .andExpect(jsonPath("$.data.list[0].priorityText").value("中"))
                .andExpect(jsonPath("$.data.list[0].dueDate").value("2026-11-30"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("GET 列表：pageSize 省略时用 profile 默认值，pageNo<1 归一到 1")
    void listShouldFallbackToDefaults() throws Exception {
        when(buss.search(any(), any(), eq(1), eq(5)))
                .thenReturn(new PageResult<>(List.of(), 1, 5, 0));

        mockMvc.perform(get(BASE).param("pageNo", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").isEmpty());
    }

    @Test
    @DisplayName("GET 列表：status 合法时转成 Byte 传给业务层")
    void listShouldPassStatusAsByte() throws Exception {
        when(buss.search(isNull(), eq((byte) 1), eq(1), eq(5)))
                .thenReturn(new PageResult<>(List.of(), 1, 5, 0));

        mockMvc.perform(get(BASE).param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("GET 列表：status=9 -> 400 / 40001，不查库（不是返回空列表）")
    void listWithIllegalStatusShouldReturn400() throws Exception {
        mockMvc.perform(get(BASE).param("status", "9"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("status 只能是 0(待办)、1(进行中)、2(已完成)"));

        verify(buss, never()).search(any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("GET 列表：pageNo 传非数字 -> 400 / 40001，且不进业务层")
    void listWithBadPageNoShouldReturn400() throws Exception {
        mockMvc.perform(get(BASE).param("pageNo", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(buss, never()).search(any(), any(), anyInt(), anyInt());
    }

    // ==================== 查询详情 ====================

    @Test
    @DisplayName("GET 详情：命中时返回 200 与任务字段（含中文名）")
    void detailShouldReturnTask() throws Exception {
        when(buss.findById(7)).thenReturn(task(7, "联调接口", (byte) 1, (byte) 3, null));

        mockMvc.perform(get(BASE + "/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.statusText").value("进行中"))
                .andExpect(jsonPath("$.data.priorityText").value("高"))
                // 未设置截止日期时给空串而不是 null，前端不用判空
                .andExpect(jsonPath("$.data.dueDate").value(""));
    }

    @Test
    @DisplayName("GET 详情：不存在 -> 404 / 40401")
    void detailNotFoundShouldReturn404() throws Exception {
        when(buss.findById(999)).thenReturn(null);

        mockMvc.perform(get(BASE + "/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401))
                .andExpect(jsonPath("$.message").value("id=999 的任务不存在"));
    }

    // ==================== 新增 ====================

    @Test
    @DisplayName("POST 新增：201 + Location 头 + 回读后的创建时间")
    void createShouldReturn201() throws Exception {
        Task created = task(100, "新任务", (byte) 0, (byte) 1, "2026-12-01");
        // 模拟 MyBatis-Plus 自增主键回填
        when(buss.add(any(Task.class))).thenAnswer(inv -> {
            inv.getArgument(0, Task.class).setId(100);
            return true;
        });
        when(buss.findById(100)).thenReturn(created);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"  新任务  ","priority":1,"status":0,"dueDate":"2026-12-01"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", BASE + "/100"))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.createDate").isNotEmpty());

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(buss).add(captor.capture());
        // 首尾空格被 trim；createDate 不由客户端提供
        assertThat(captor.getValue().getTitle()).isEqualTo("新任务");
        assertThat(captor.getValue().getCreateDate()).isNull();
        // id 是「数据库回填」的：这里由 add() 的打桩 Answer 模拟，回填后应为 100
        assertThat(captor.getValue().getId()).isEqualTo(100);
    }

    @Test
    @DisplayName("POST 新增：标题为空 -> 400 / 40001，不写库")
    void createWithBlankTitleShouldReturn400() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"   ","priority":1,"status":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("title: 任务标题不能为空"));

        verify(buss, never()).add(any());
    }

    @Test
    @DisplayName("POST 新增：优先级越界 -> 400 / 40001")
    void createWithIllegalPriorityShouldReturn400() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"任务","priority":9,"status":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("priority: 优先级只能是 1(低)、2(中)、3(高)"));
    }

    @Test
    @DisplayName("POST 新增：2026-02-30 能过正则但不是真实日期 -> 400 / 40002")
    void createWithImpossibleDateShouldReturn40002() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"任务","priority":1,"status":0,"dueDate":"2026-02-30"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40002));
    }

    @Test
    @DisplayName("POST 新增：请求体不是合法 JSON -> 400 / 40001")
    void createWithBrokenJsonShouldReturn40001() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{title:}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    // ==================== 更新 ====================

    @Test
    @DisplayName("PUT 更新：成功返回更新后的任务，含原有创建时间")
    void updateShouldReturnTask() throws Exception {
        Task existing = task(7, "旧标题", (byte) 0, (byte) 1, "2026-11-01");
        when(buss.findById(7)).thenReturn(existing);
        when(buss.update(any(Task.class))).thenReturn(true);

        mockMvc.perform(put(BASE + "/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"新标题","priority":2,"status":1,"dueDate":"2026-11-20"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.title").value("新标题"))
                .andExpect(jsonPath("$.data.statusText").value("进行中"))
                .andExpect(jsonPath("$.data.createDate").isNotEmpty());
    }

    @Test
    @DisplayName("PUT 更新：目标不存在 -> 404 / 40401")
    void updateMissingTaskShouldReturn404() throws Exception {
        when(buss.findById(8)).thenReturn(null);

        mockMvc.perform(put(BASE + "/8")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"新标题","priority":2,"status":1}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));

        verify(buss, never()).update(any());
    }

    @Test
    @DisplayName("PUT 更新：影响行数为 0 -> 500 / 50003")
    void updateAffectingNoRowShouldReturn50003() throws Exception {
        when(buss.findById(7)).thenReturn(task(7, "旧标题", (byte) 0, (byte) 1, null));
        when(buss.update(any(Task.class))).thenReturn(false);

        mockMvc.perform(put(BASE + "/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"新标题","priority":2,"status":1}
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(50003));
    }

    // ==================== 状态流转 ====================

    @Test
    @DisplayName("PATCH 状态：只改 status，其他字段原样保留")
    void patchStatusShouldOnlyChangeStatus() throws Exception {
        Task existing = task(7, "联调接口", (byte) 0, (byte) 3, "2026-11-30");
        when(buss.findById(7)).thenReturn(existing);
        when(buss.update(any(Task.class))).thenReturn(true);

        mockMvc.perform(patch(BASE + "/7/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(2))
                .andExpect(jsonPath("$.data.statusText").value("已完成"))
                .andExpect(jsonPath("$.data.title").value("联调接口"));

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(buss).update(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("联调接口");
        assertThat(captor.getValue().getStatus()).isEqualTo((byte) 2);
    }

    @Test
    @DisplayName("PATCH 状态：status=9 -> 400 / 40001")
    void patchIllegalStatusShouldReturn400() throws Exception {
        mockMvc.perform(patch(BASE + "/7/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":9}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    // ==================== 删除 ====================

    @Test
    @DisplayName("DELETE：成功时 data 为 null")
    void deleteShouldReturnEmptyData() throws Exception {
        when(buss.delete(7)).thenReturn(true);

        mockMvc.perform(delete(BASE + "/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("DELETE：不存在 -> 404 / 40401")
    void deleteMissingTaskShouldReturn404() throws Exception {
        when(buss.delete(8)).thenReturn(false);

        mockMvc.perform(delete(BASE + "/8"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40401));
    }

    // ==================== 异常分支 ====================

    @Test
    @DisplayName("数据库不可用 -> 503 / 50002，而不是笼统的 500")
    void dataAccessErrorShouldReturn50002() throws Exception {
        when(buss.findById(1)).thenThrow(new DataAccessResourceFailureException("Could not get JDBC Connection"));

        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(50002));
    }

    @Test
    @DisplayName("未预期异常 -> 500 / 50001，且不把堆栈返回给客户端")
    void unexpectedErrorShouldReturn50001() throws Exception {
        when(buss.findById(1)).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(50001))
                .andExpect(jsonPath("$.message").value("系统繁忙，请稍后重试"));
    }
}
