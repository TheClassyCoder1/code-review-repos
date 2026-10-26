package com.example.lending.ops.part5.maintenance.web;

import lombok.Getter;

/** Paging input shared by console list endpoints. */
@Getter
public class PageParam {

    /** Disables paging; only used by exports, which stream the full result set. */
    public static final int PAGE_SIZE_NONE = -1;

    private int pageNo = 1;
    private int pageSize = 10;

    public void setPageNo(int pageNo) {
        this.pageNo = Math.max(1, pageNo);
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize == PAGE_SIZE_NONE ? PAGE_SIZE_NONE : Math.min(200, Math.max(1, pageSize));
    }
}
