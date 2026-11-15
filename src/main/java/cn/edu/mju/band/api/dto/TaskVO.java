package cn.edu.mju.band.api.dto;

import cn.edu.mju.band.entity.Task;
import cn.edu.mju.band.util.TaskFormatter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 任务出参对象（VO = View Object）。
 *
 * <p><b>为什么不直接返回 {@link Task} 实体</b>：实体是数据库结构的一比一映射，
 * 一旦表加了内部字段（比如 is_deleted、operator_id），直接返回就会把它们一起暴露出去；
 * 反过来，前端要的"状态中文名"又不在实体里。VO 是接口的契约，实体是存储的契约，
 * 两者的变更节奏不同，必须解耦。</p>
 *
 * <p><b>日期为什么输出成字符串</b>：{@code java.util.Date} 默认会被 Jackson 序列化成
 * 时间戳数字（1699999999999），前端还得自己 new Date()。直接给
 * {@code 2026-11-30} 这种人类可读格式，字段含义从肉眼就能确认。</p>
 *
 * <p><b>为什么不复用 {@link TaskFormatter#formatDate}</b>：它是为 Thymeleaf 页面写的，
 * 内部持有 <b>静态</b> {@code SimpleDateFormat}。该类的 format/parse 方法都不是线程安全的，
 * Web 容器里多个请求线程共用一个实例，高并发下会出现日期串号。
 * 本类改用不可变的 {@link DateTimeFormatter}，天然线程安全（该问题已记入评审清单）。</p>
 */
@Schema(description = "任务信息")
public class TaskVO {

    /** DateTimeFormatter 是不可变对象，可以安全地作为静态常量复用 */
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Schema(description = "任务 ID", example = "1")
    private Integer id;

    @Schema(description = "任务标题", example = "完成 DevOps 实验报告")
    private String title;

    @Schema(description = "状态码：0=待办 1=进行中 2=已完成", example = "0")
    private Byte status;

    @Schema(description = "状态中文名", example = "待办")
    private String statusText;

    @Schema(description = "优先级：1=低 2=中 3=高", example = "2")
    private Byte priority;

    @Schema(description = "优先级中文名", example = "中")
    private String priorityText;

    @Schema(description = "截止日期 yyyy-MM-dd，未设置为空字符串", example = "2026-11-30")
    private String dueDate;

    @Schema(description = "创建时间 yyyy-MM-dd HH:mm:ss", example = "2026-11-15 21:30:00")
    private String createDate;

    /**
     * 实体 -> VO。所有字段转换集中在这一个方法里，
     * 以后加字段只需要改一处，不会出现"某个接口漏填了 statusText"。
     */
    public static TaskVO from(Task task) {
        if (task == null) {
            return null;
        }
        TaskVO vo = new TaskVO();
        vo.setId(task.getId());
        vo.setTitle(task.getTitle());
        vo.setStatus(task.getStatus());
        vo.setStatusText(TaskFormatter.getStatusText(task.getStatus()));
        vo.setPriority(task.getPriority());
        vo.setPriorityText(TaskFormatter.getPriorityText(task.getPriority()));
        vo.setDueDate(formatDate(task.getDueDate()));
        vo.setCreateDate(formatDateTime(task.getCreateDate()));
        return vo;
    }

    private static String formatDate(Date date) {
        return date == null ? "" : toLocalDateTime(date).toLocalDate().format(DATE_FMT);
    }

    private static String formatDateTime(Date date) {
        return date == null ? "" : toLocalDateTime(date).format(DATETIME_FMT);
    }

    /**
     * java.util.Date -> LocalDateTime。
     * 注意不能写 {@code date.toInstant()}：MySQL 的 DATE / DATETIME 列经 JDBC 读出来是
     * {@code java.sql.Date} / {@code java.sql.Timestamp}，而 java.sql.Date 覆写了
     * toInstant() 直接抛 UnsupportedOperationException。用毫秒值换算对所有子类都安全。
     */
    private static LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault());
    }

    /** 供测试与调用方构造空对象 */
    public TaskVO() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Byte getStatus() {
        return status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public Byte getPriority() {
        return priority;
    }

    public void setPriority(Byte priority) {
        this.priority = priority;
    }

    public String getPriorityText() {
        return priorityText;
    }

    public void setPriorityText(String priorityText) {
        this.priorityText = priorityText;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getCreateDate() {
        return createDate;
    }

    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }
}
