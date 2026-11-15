package cn.edu.mju.band.api;

import cn.edu.mju.band.api.dto.PageVO;
import cn.edu.mju.band.api.dto.TaskRequest;
import cn.edu.mju.band.api.dto.TaskStatusRequest;
import cn.edu.mju.band.api.dto.TaskVO;
import cn.edu.mju.band.buss.ITaskBuss;
import cn.edu.mju.band.common.ApiException;
import cn.edu.mju.band.common.ErrorCode;
import cn.edu.mju.band.common.Result;
import cn.edu.mju.band.entity.PageResult;
import cn.edu.mju.band.entity.Task;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务 REST 接口（实验环节二）。
 *
 * <p><b>为什么另建一个 Controller 而不是改造 TaskController</b>：{@code TaskController}
 * 返回的是视图名（{@code "task"} / {@code "redirect:/task"}），由 Thymeleaf 渲染 HTML；
 * 本类返回 JSON。两者的错误处理、认证方式、出参形状都不同，混在一个类里
 * 只会让每个方法都用 if 判断"这次是页面还是接口"。分包之后，
 * 页面请求由 {@code GlobalExceptionHandler} 返回错误页，
 * 接口请求由 {@link ApiExceptionHandler} 返回 JSON，互不干扰。</p>
 *
 * <p><b>URL 版本号 /api/v1</b>：接口一旦被第三方调用就不能随便改形状。
 * 需要不兼容变更时新开 /api/v2，旧版本继续服务一段时间，
 * 客户端可以按自己的节奏迁移。</p>
 *
 * <p><b>业务逻辑全部复用 {@link ITaskBuss}</b>，本类只做三件事：
 * 参数解码、调用业务层、把实体翻译成 VO。任何"顺手算一下"的逻辑都应该下沉到业务层，
 * 否则页面和接口会各写一份，迟早不一致。</p>
 */
@Tag(name = "任务管理", description = "任务的增删改查与状态流转，所有响应均为统一结构 {code, message, data, timestamp}")
@RestController
@RequestMapping("/api/v1/tasks")
public class ApiTaskController {

    private final ITaskBuss buss;

    /**
     * 每页默认条数来自当前 profile（dev=5 / test=3 / prod=10），
     * 与页面控制器读同一个配置项，避免"接口和页面分页条数不一样"。
     */
    private final int defaultPageSize;

    public ApiTaskController(ITaskBuss buss,
                             @Value("${app.task.default-page-size:5}") int defaultPageSize) {
        this.buss = buss;
        this.defaultPageSize = defaultPageSize;
    }

    @Operation(summary = "分页查询任务",
            description = "支持按标题模糊匹配、按状态精确过滤，两者都可省略。"
                    + "页码越界时自动回到最后一页；pageSize 超过上限（100）时回落为默认值。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功，data 为分页对象"),
            @ApiResponse(responseCode = "400", description = "参数类型不合法，如 pageNo=abc",
                    content = @Content(schema = @Schema(implementation = Result.class)))
    })
    @GetMapping
    public Result<PageVO<TaskVO>> list(
            @Parameter(description = "任务标题，模糊匹配，可为空", example = "报告")
            @RequestParam(required = false) String title,
            // 用 Integer 而不是 Byte 接收：Byte 在 OpenAPI 里会被描述成 string(byte)，
            // Swagger UI 据此渲染出一个"选择文件"控件，调用方完全没法用。
            // 类型转换与范围校验放在方法里显式做，错误码也更明确。
            @Parameter(description = "状态：0=待办 1=进行中 2=已完成，可为空", example = "0")
            @RequestParam(required = false) Integer status,
            @Parameter(description = "页码，从 1 开始，默认 1", example = "1")
            @RequestParam(required = false) Integer pageNo,
            @Parameter(description = "每页条数 1~100，省略时取当前环境默认值", example = "5")
            @RequestParam(required = false) Integer pageSize) {

        int size = (pageSize == null || pageSize < 1) ? defaultPageSize : pageSize;
        int no = (pageNo == null || pageNo < 1) ? 1 : pageNo;

        PageResult<Task> page = buss.search(title, toStatus(status), no, size);
        List<TaskVO> list = page.getList().stream().map(TaskVO::from).collect(Collectors.toList());
        return Result.ok(new PageVO<>(list, page.getPageNo(), page.getPageSize(), page.getTotalCount()));
    }

    /**
     * 查询条件里的 status 校验：只接受 0/1/2。
     * 不合法时直接报 40001，而不是拿去查库后返回空列表 ——
     * "查不到数据"和"参数写错了"是两件事，不能都表现成空列表。
     */
    private Byte toStatus(Integer status) {
        if (status == null) {
            return null;
        }
        if (status < 0 || status > 2) {
            throw new ApiException(ErrorCode.PARAM_INVALID, "status 只能是 0(待办)、1(进行中)、2(已完成)");
        }
        return status.byteValue();
    }

    @Operation(summary = "按 ID 查询任务")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "任务不存在，code=40401")
    })
    @GetMapping("/{id}")
    public Result<TaskVO> detail(
            @Parameter(description = "任务 ID", example = "1") @PathVariable Integer id) {
        Task task = buss.findById(id);
        if (task == null) {
            throw new ApiException(ErrorCode.TASK_NOT_FOUND, "id=" + id + " 的任务不存在");
        }
        return Result.ok(TaskVO.from(task));
    }

    @Operation(summary = "新增任务", description = "成功返回 201 Created，响应头 Location 指向新资源")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "创建成功，data 含数据库生成的自增 ID"),
            @ApiResponse(responseCode = "400", description = "字段校验失败，code=40001/40002")
    })
    @PostMapping
    public ResponseEntity<Result<TaskVO>> create(@Valid @RequestBody TaskRequest request) {
        Task task = toEntity(request);
        if (!buss.add(task)) {
            throw new ApiException(ErrorCode.WRITE_FAILED);
        }
        // 写后回读一次：create_date 由数据库 DEFAULT CURRENT_TIMESTAMP 生成，
        // 不回读就只能给客户端返回空字符串。这一条 SELECT 换接口契约的完整性，值得。
        Task created = buss.findById(task.getId());
        TaskVO vo = TaskVO.from(created == null ? task : created);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Location", "/api/v1/tasks/" + task.getId())
                .body(Result.ok(vo));
    }

    @Operation(summary = "全量更新任务",
            description = "所有字段必填；dueDate 传 null 或空串表示清空截止日期。"
                    + "只改状态请用 PATCH /{id}/status，避免误清空其他字段。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功，data 为更新后的任务"),
            @ApiResponse(responseCode = "400", description = "字段校验失败，code=40001/40002"),
            @ApiResponse(responseCode = "404", description = "任务不存在，code=40401")
    })
    @PutMapping("/{id}")
    public Result<TaskVO> update(@Parameter(description = "任务 ID", example = "1") @PathVariable Integer id,
                                 @Valid @RequestBody TaskRequest request) {
        Task existing = buss.findById(id);
        if (existing == null) {
            throw new ApiException(ErrorCode.TASK_NOT_FOUND, "id=" + id + " 的任务不存在");
        }

        Task task = toEntity(request);
        task.setId(id);
        // 带上原有的 create_date：实体的 updateStrategy=NEVER 保证它不会被写进 SQL，
        // 但出参能原样带回去，客户端不会看到"创建时间突然变成空"。
        task.setCreateDate(existing.getCreateDate());

        if (!buss.update(task)) {
            throw new ApiException(ErrorCode.WRITE_FAILED);
        }
        return Result.ok(TaskVO.from(task));
    }

    @Operation(summary = "更新任务状态", description = "只修改 status 一个字段，用于看板拖拽、待办勾选这类场景")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "400", description = "status 越界，code=40001"),
            @ApiResponse(responseCode = "404", description = "任务不存在，code=40401")
    })
    @PatchMapping("/{id}/status")
    public Result<TaskVO> updateStatus(@Parameter(description = "任务 ID", example = "1") @PathVariable Integer id,
                                       @Valid @RequestBody TaskStatusRequest request) {
        Task existing = buss.findById(id);
        if (existing == null) {
            throw new ApiException(ErrorCode.TASK_NOT_FOUND, "id=" + id + " 的任务不存在");
        }
        existing.setStatus(request.getStatus());
        if (!buss.update(existing)) {
            throw new ApiException(ErrorCode.WRITE_FAILED);
        }
        return Result.ok(TaskVO.from(existing));
    }

    @Operation(summary = "删除任务", description = "物理删除；成功时 data 为 null")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "任务不存在，code=40401")
    })
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "任务 ID", example = "1") @PathVariable Integer id) {
        if (!buss.delete(id)) {
            throw new ApiException(ErrorCode.TASK_NOT_FOUND, "id=" + id + " 的任务不存在");
        }
        return Result.ok();
    }

    /** 入参 -> 实体。opt/id/createDate 这些客户端无权决定的字段一个都不碰。 */
    private Task toEntity(TaskRequest request) {
        Task task = new Task();
        task.setTitle(request.getTitle().trim());   // @NotBlank 已保证非 null
        task.setPriority(request.getPriority());
        task.setStatus(request.getStatus());
        task.setDueDate(parseDate(request.getDueDate()));
        return task;
    }

    /**
     * yyyy-MM-dd -> Date。
     *
     * <p>用 {@code java.sql.Date.valueOf(LocalDate)} 而不是 {@code new SimpleDateFormat().parse()}：
     * 后者是非线程安全的实例方法（每次都得 new，白白产生对象），而且默认宽松模式会把
     * "2026-02-30" 悄悄滚成 3 月 2 日。LocalDate 严格解析，不存在的日期直接报错，
     * 正好对应 40002。</p>
     */
    private Date parseDate(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return java.sql.Date.valueOf(LocalDate.parse(text.trim()));
        } catch (DateTimeParseException e) {
            throw new ApiException(ErrorCode.DATE_FORMAT_INVALID, "截止日期不是真实存在的日期: " + text);
        }
    }
}
