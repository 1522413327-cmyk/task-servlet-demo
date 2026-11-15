package cn.edu.mju.band.controller;

import cn.edu.mju.band.buss.ITaskBuss;
import cn.edu.mju.band.entity.Task;
import cn.edu.mju.band.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 环节四的关键证据：模拟「绕过客户端校验」——不经过浏览器，直接 POST 非法数据，
 * 验证 @Valid 在服务端拦下、BindingResult 产生字段错误、且没有写进数据库。
 * 用 standaloneSetup 只装载 TaskController + 统一异常处理器，不启动 Spring 容器、不连库。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("绕过客户端校验后的服务端拦截（MockMvc）")
class TaskValidationMvcTest {

    @Mock
    private ITaskBuss buss;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TaskController controller = new TaskController();
        ReflectionTestUtils.setField(controller, "buss", buss);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("直接 POST 空标题：服务端拦下、回显字段错误、不写库")
    void blankTitleShouldBeRejectedByServer() throws Exception {
        mockMvc.perform(post("/task")
                        .param("opt", "add")
                        .param("title", "")
                        .param("priority", "1")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("add_update"))
                .andExpect(model().attributeHasFieldErrors("taskForm", "title"))
                .andExpect(model().attribute("opt", "add"));

        verifyNoInteractions(buss);
    }

    @Test
    @DisplayName("直接 POST 非法日期：@Pattern 拦下并回显 dueDate 字段错误")
    void invalidDateShouldBeRejectedByServer() throws Exception {
        mockMvc.perform(post("/task")
                        .param("opt", "add")
                        .param("title", "写报告")
                        .param("priority", "1")
                        .param("status", "1")
                        .param("dueDate", "not-a-date"))
                .andExpect(status().isOk())
                .andExpect(view().name("add_update"))
                .andExpect(model().attributeHasFieldErrors("taskForm", "dueDate"));

        verifyNoInteractions(buss);
    }

    @Test
    @DisplayName("缺少 priority / status（绕过下拉框）也被 @NotNull 拦下")
    void missingSelectsShouldBeRejectedByServer() throws Exception {
        mockMvc.perform(post("/task")
                        .param("opt", "add")
                        .param("title", "写报告"))
                .andExpect(status().isOk())
                .andExpect(view().name("add_update"))
                .andExpect(model().attributeHasFieldErrors("taskForm", "priority", "status"));

        verifyNoInteractions(buss);
    }

    @Test
    @DisplayName("合法表单：校验通过、标题 trim 后入库、重定向列表")
    void validFormShouldBeSaved() throws Exception {
        mockMvc.perform(post("/task")
                        .param("opt", "add")
                        .param("title", "  写实验报告  ")
                        .param("priority", "2")
                        .param("status", "1")
                        .param("dueDate", "2026-11-20"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/task"));

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(buss).add(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("写实验报告");
        assertThat(captor.getValue().getDueDate()).isNotNull();
    }
}
