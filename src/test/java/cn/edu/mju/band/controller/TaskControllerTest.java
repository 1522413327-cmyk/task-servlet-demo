package cn.edu.mju.band.controller;

import cn.edu.mju.band.buss.ITaskBuss;
import cn.edu.mju.band.entity.PageResult;
import cn.edu.mju.band.entity.Task;
import cn.edu.mju.band.form.TaskForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.text.SimpleDateFormat;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * TaskController 的纯单元测试：业务层打桩、Model 用内存实现，不启动 Spring 容器。
 * 环节四改造后 save() 改为 @Valid + BindingResult + TaskForm 的签名，
 * 这里用 BeanPropertyBindingResult 模拟「校验通过 / 校验失败」两种输入。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskController 单元测试")
class TaskControllerTest {

    @Mock
    private ITaskBuss buss;

    @InjectMocks
    private TaskController controller;

    private Model model;

    @BeforeEach
    void setUp() {
        model = new ExtendedModelMap();
    }

    private TaskForm form(String opt, String title, String dueDate, Integer id) {
        TaskForm f = new TaskForm();
        f.setOpt(opt);
        f.setTitle(title);
        f.setPriority((byte) 2);
        f.setStatus((byte) 1);
        f.setDueDate(dueDate);
        f.setId(id);
        return f;
    }

    private BindingResult noErrors(TaskForm f) {
        return new BeanPropertyBindingResult(f, "taskForm");
    }

    @Test
    @DisplayName("list(): 把分页五要素与查询条件放进 Model")
    void listShouldExposePageInfoToView() {
        Task task = new Task();
        task.setId(1);
        task.setTitle("写实验报告");
        when(buss.search(any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResult<>(Arrays.asList(task), 2, 5, 12));

        String view = controller.list("报告", (byte) 1, 2, 5, model);

        assertThat(view).isEqualTo("task");
        assertThat(model.asMap().get("lst")).isEqualTo(Arrays.asList(task));
        assertThat(model.asMap().get("pageNo")).isEqualTo(2);
        assertThat(model.asMap().get("pageSize")).isEqualTo(5);
        assertThat(model.asMap().get("totalCount")).isEqualTo(12);
        assertThat(model.asMap().get("totalPages")).isEqualTo(3);
        assertThat(model.asMap().get("title")).isEqualTo("报告");
        assertThat(model.asMap().get("status")).isEqualTo((byte) 1);
        verify(buss).search("报告", (byte) 1, 2, 5);
    }

    @Test
    @DisplayName("list(): 参数缺省时页码回落 1、每页条数取配置默认值 5")
    void listShouldUseDefaultsWhenParamsMissing() {
        when(buss.search(any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageResult<>());

        controller.list(null, null, null, null, model);

        // 非 Spring 环境下 @Value 字段取初始化值 5，与 application.yml 默认值一致
        verify(buss).search(null, null, 1, 5);
    }

    @Test
    @DisplayName("addForm(): 绑定 taskForm 并预置默认优先级与状态")
    void addFormShouldPrepareDefaults() {
        assertThat(controller.addForm(model)).isEqualTo("add_update");
        assertThat(model.asMap().get("opt")).isEqualTo("add");
        TaskForm form = (TaskForm) model.asMap().get("taskForm");
        assertThat(form.getOpt()).isEqualTo("add");
        assertThat(form.getPriority()).isEqualTo((byte) 1);
        assertThat(form.getStatus()).isEqualTo((byte) 1);
    }

    @Test
    @DisplayName("updateForm(): 实体转表单对象，日期格式化为 yyyy-MM-dd；找不到则重定向")
    void updateFormShouldConvertEntityToForm() throws Exception {
        Task task = new Task();
        task.setId(7);
        task.setTitle("改任务");
        task.setPriority((byte) 2);
        task.setStatus((byte) 1);
        task.setDueDate(new SimpleDateFormat("yyyy-MM-dd").parse("2026-12-01"));
        when(buss.findById(7)).thenReturn(task);
        when(buss.findById(8)).thenReturn(null);

        assertThat(controller.updateForm(7, model)).isEqualTo("add_update");
        TaskForm form = (TaskForm) model.asMap().get("taskForm");
        assertThat(form.getId()).isEqualTo(7);
        assertThat(form.getOpt()).isEqualTo("update");
        assertThat(form.getTitle()).isEqualTo("改任务");
        assertThat(form.getPriority()).isEqualTo((byte) 2);
        assertThat(form.getDueDate()).isEqualTo("2026-12-01");
        assertThat(controller.updateForm(8, model)).isEqualTo("redirect:/task");
    }

    @Test
    @DisplayName("delete(): 调用业务层后重定向列表")
    void deleteShouldDelegateAndRedirect() {
        assertThat(controller.delete(7)).isEqualTo("redirect:/task");
        verify(buss).delete(7);
    }

    @Test
    @DisplayName("save(): opt=add / opt=update 分别走对应业务方法，日期正确解析")
    void saveShouldDispatchByOpt() {
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        TaskForm add = form("add", "新任务", "2026-11-04", null);
        TaskForm update = form("update", "改任务", "2026-12-01", 9);

        assertThat(controller.save(add, noErrors(add), model)).isEqualTo("redirect:/task");
        assertThat(controller.save(update, noErrors(update), model)).isEqualTo("redirect:/task");

        verify(buss).add(captor.capture());
        Task added = captor.getValue();
        assertThat(added.getTitle()).isEqualTo("新任务");
        assertThat(added.getPriority()).isEqualTo((byte) 2);
        assertThat(added.getDueDate()).isNotNull();
        assertThat(added.getId()).isNull();

        ArgumentCaptor<Task> updateCaptor = ArgumentCaptor.forClass(Task.class);
        verify(buss).update(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getId()).isEqualTo(9);
        assertThat(updateCaptor.getValue().getTitle()).isEqualTo("改任务");
    }

    @Test
    @DisplayName("save(): 未知 opt 不落库，只重定向")
    void saveShouldIgnoreUnknownOpt() {
        TaskForm f = form("unknown", "x", null, null);
        assertThat(controller.save(f, noErrors(f), model)).isEqualTo("redirect:/task");
        verify(buss, never()).add(any());
        verify(buss, never()).update(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("save(): 空日期被安全解析为 null，不抛异常")
    void saveShouldTreatEmptyDateAsNull(String dueDate) {
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        TaskForm f = form("add", "任务", dueDate, null);

        assertThat(controller.save(f, noErrors(f), model)).isEqualTo("redirect:/task");

        verify(buss).add(captor.capture());
        assertThat(captor.getValue().getDueDate()).isNull();
    }

    @Test
    @DisplayName("save(): 标题首尾空白入库前被 trim")
    void saveShouldTrimTitle() {
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        TaskForm f = form("add", "  写实验报告  ", "", null);

        assertThat(controller.save(f, noErrors(f), model)).isEqualTo("redirect:/task");

        verify(buss).add(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("写实验报告");
    }

    @Test
    @DisplayName("save(): BindingResult 有错时回原表单，保留用户填错的内容，且不写库")
    void saveShouldReturnFormWhenValidationFails() {
        TaskForm f = form("add", "", "not-a-date", null);
        BindingResult errors = new BeanPropertyBindingResult(f, "taskForm");
        errors.addError(new FieldError("taskForm", "title", "任务标题不能为空"));
        errors.addError(new FieldError("taskForm", "dueDate", "截止日期格式应为 yyyy-MM-dd"));

        assertThat(controller.save(f, errors, model)).isEqualTo("add_update");
        assertThat(model.asMap().get("opt")).isEqualTo("add");
        assertThat(model.asMap().get("taskForm")).isSameAs(f);
        assertThat(f.getDueDate()).isEqualTo("not-a-date");   // 原始输入未被清空，供回显
        verifyNoInteractions(buss);
    }
}
