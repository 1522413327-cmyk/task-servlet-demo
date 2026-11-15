package cn.edu.mju.band.controller;

import cn.edu.mju.band.buss.ITaskBuss;
import cn.edu.mju.band.entity.PageResult;
import cn.edu.mju.band.entity.Task;
import cn.edu.mju.band.form.TaskForm;
import cn.edu.mju.band.util.TaskFormatter;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Controller
@RequestMapping("/task")
public class TaskController {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    private ITaskBuss buss;

    /**
     * 每页默认条数不再写死 5，改为读当前 profile 的 app.task.default-page-size。
     * 注解的 defaultValue 只能填编译期常量，所以这里改成 required=false + 运行时兜底。
     */
    @Value("${app.task.default-page-size:5}")
    private int defaultPageSize = 5;

    @GetMapping
    public String list(@RequestParam(value = "title", required = false) String title,
                       @RequestParam(value = "status", required = false) Byte status,
                       @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       Model model) {
        PageResult<Task> page = buss.search(title, status,
                pageNo == null ? 1 : pageNo,
                pageSize == null ? defaultPageSize : pageSize);
        // 拆成独立属性放入 Model：页面只依赖 List/Task，不引用 PageResult
        model.addAttribute("lst", page.getList());
        model.addAttribute("pageNo", page.getPageNo());
        model.addAttribute("pageSize", page.getPageSize());
        model.addAttribute("totalCount", page.getTotalCount());
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("title", title);      // 查询条件回显
        model.addAttribute("status", status);
        return "task";                                  // -> classpath:/templates/task.html
    }

    /** 环节四：GET 表单也用 TaskForm，保证 th:object/th:field 在新增页就能绑定。 */
    @GetMapping(params = "opt=add")
    public String addForm(Model model) {
        TaskForm form = new TaskForm();
        form.setOpt("add");
        form.setPriority((byte) 1);
        form.setStatus((byte) 1);
        model.addAttribute("opt", "add");
        model.addAttribute("taskForm", form);
        return "add_update";                            // -> classpath:/templates/add_update.html
    }

    @GetMapping(params = "opt=update")
    public String updateForm(@RequestParam("id") Integer id, Model model) {
        Task task = buss.findById(id);
        if (task == null) {
            return "redirect:/task";
        }
        model.addAttribute("opt", "update");
        model.addAttribute("taskForm", toForm(task, "update"));
        return "add_update";
    }

    @GetMapping(params = "opt=delete")
    public String delete(@RequestParam("id") Integer id) {
        buss.delete(id);
        return "redirect:/task";                         // Spring 自动加 contextPath
    }

    /**
     * 环节四：服务端校验 + 表单错误回显。
     *
     * <p>{@code @Valid} 触发 TaskForm 上的约束校验，紧跟其后的 {@code BindingResult}
     * 接收错误（顺序不能颠倒，否则 Spring 会直接抛 BindException 交给
     * {@code GlobalExceptionHandler}）。校验失败时返回原表单视图，
     * Spring 会自动把 taskForm 与 BindingResult 放进 Model，
     * 模板用 {@code th:field} 回填输入、{@code th:errors} 显示中文错误消息。</p>
     *
     * <p>这一步是客户端校验绕不过去的最后防线：浏览器可以跳过 required，
     * 也可以直接用 curl POST 空标题，但请求一定会被这里的 @Valid 拦下，
     * 不会写进数据库。</p>
     */
    @PostMapping
    public String save(@Valid @ModelAttribute("taskForm") TaskForm form,
                       BindingResult bindingResult,
                       Model model) {
        if (bindingResult.hasErrors()) {
            // 保留 opt 与表单对象，让页面能原样再渲染一次（含用户填错的内容）。
            // Spring MVC 会自动放一次，这里显式写也让纯单测能验证回显内容。
            model.addAttribute("opt", form.getOpt());
            model.addAttribute("taskForm", form);
            return "add_update";
        }

        Task task = new Task();
        task.setId(form.getId());
        task.setTitle(form.getTitle().trim());       // @NotBlank 已保证非 null
        task.setPriority(form.getPriority());
        task.setStatus(form.getStatus());
        task.setDueDate(parseDate(form.getDueDate()));
        if ("add".equals(form.getOpt())) {
            buss.add(task);
        } else if ("update".equals(form.getOpt())) {
            buss.update(task);
        }
        return "redirect:/task";
    }

    /** 实体 -> 表单对象：日期转成 yyyy-MM-dd 字符串，供 type=date 输入框显示。 */
    private TaskForm toForm(Task task, String opt) {
        TaskForm form = new TaskForm();
        form.setId(task.getId());
        form.setOpt(opt);
        form.setTitle(task.getTitle());
        form.setPriority(task.getPriority());
        form.setStatus(task.getStatus());
        form.setDueDate(TaskFormatter.formatDate(task.getDueDate()));
        return form;
    }

    private Date parseDate(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return SDF.parse(s.trim());
        } catch (ParseException e) {
            return null;
        }
    }
}
