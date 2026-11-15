package cn.edu.mju.band.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 视图层格式化工具：将 priority/status 的魔法数字映射为中文，
 * 并将 Date 统一格式化为 yyyy-MM-dd，消除模板里散落的判断。
 *
 * <p>由 JSP 迁到 Thymeleaf 后，模板通过 SpringEL 的静态方法调用：
 * {@code ${T(cn.edu.mju.band.util.TaskFormatter).getStatusText(t.status)}}。
 * 参数类型由 byte 改为包装类 Byte 并做 null 判断，避免实体字段为空时拆箱抛 NPE。
 */
public final class TaskFormatter {
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");

    private TaskFormatter() {
    }

    public static String getPriorityText(Byte priority) {
        if (priority == null) {
            return "";
        }
        switch (priority) {
            case 1:
                return "低";
            case 2:
                return "中";
            case 3:
                return "高";
            default:
                return String.valueOf(priority);
        }
    }

    public static String getStatusText(Byte status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case 0:
                return "待办";
            case 1:
                return "进行中";
            case 2:
                return "已完成";
            default:
                return String.valueOf(status);
        }
    }

    public static String formatDate(Date date) {
        return date == null ? "" : DATE_FMT.format(date);
    }
}
