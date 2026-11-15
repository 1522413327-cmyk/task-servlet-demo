package cn.edu.mju.band.buss;

import cn.edu.mju.band.entity.PageResult;
import cn.edu.mju.band.entity.Task;
import java.util.List;

public interface ITaskBuss {
    List<Task> list();
    boolean add(Task task);
    boolean update(Task task);
    Task findById(int id);
    boolean delete(int id);

    /**
     * 条件 + 分页查询。
     * @param title    任务名称（模糊，可为 null）
     * @param status   任务状态（可为 null）
     * @param pageNo   页码，从 1 开始
     * @param pageSize 每页条数
     */
    PageResult<Task> search(String title, Byte status, int pageNo, int pageSize);
}
