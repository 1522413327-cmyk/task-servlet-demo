package cn.edu.mju.band.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * 分页出参。
 *
 * <p><b>为什么不复用 {@code cn.edu.mju.band.entity.PageResult}</b>：那个类是为 Thymeleaf 页面设计的，
 * 额外暴露了 {@code hasPrev} / {@code hasNext} 两个给模板做按钮置灰用的属性；
 * 而接口调用方真正需要的是"总条数"和"总页数"，自己就能算出有没有上一页。
 * 把视图模型直接当接口契约，等于让前端继承页面的历史包袱。</p>
 *
 * @param <T> 列表元素类型
 */
@Schema(description = "分页结果")
public class PageVO<T> {

    @Schema(description = "当前页数据")
    private List<T> list = new ArrayList<>();

    @Schema(description = "当前页码，从 1 开始", example = "1")
    private int pageNo;

    @Schema(description = "每页条数", example = "5")
    private int pageSize;

    @Schema(description = "符合条件的总条数", example = "30")
    private int totalCount;

    @Schema(description = "总页数", example = "6")
    private int totalPages;

    public PageVO() {
    }

    public PageVO(List<T> list, int pageNo, int pageSize, int totalCount) {
        this.list = list == null ? new ArrayList<>() : list;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
        // 向上取整；pageSize 非法时给 0，避免除零
        this.totalPages = pageSize <= 0 ? 0 : (totalCount + pageSize - 1) / pageSize;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public int getPageNo() {
        return pageNo;
    }

    public void setPageNo(int pageNo) {
        this.pageNo = pageNo;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
