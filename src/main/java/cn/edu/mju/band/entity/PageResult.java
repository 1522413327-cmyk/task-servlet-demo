package cn.edu.mju.band.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用分页结果载体。
 * 业务层算好页码/总数后塞进来，Controller 直接丢给 JSP 渲染。
 */
public class PageResult<T> {

    private List<T> list;
    private int pageNo;
    private int pageSize;
    private int totalCount;
    private int totalPages;

    public PageResult() {
        this.list = new ArrayList<>();
        this.pageNo = 1;
        this.pageSize = 5;
        this.totalPages = 1;
    }

    public PageResult(List<T> list, int pageNo, int pageSize, int totalCount) {
        this.list = list == null ? new ArrayList<>() : list;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
        this.totalPages = pageSize <= 0 ? 0 : (totalCount + pageSize - 1) / pageSize;
    }

    /** 是否有上一页 */
    public boolean isHasPrev() {
        return pageNo > 1;
    }

    /** 是否有下一页 */
    public boolean isHasNext() {
        return pageNo < totalPages;
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
