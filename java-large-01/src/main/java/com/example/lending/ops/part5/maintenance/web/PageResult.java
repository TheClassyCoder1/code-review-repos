package com.example.lending.ops.part5.maintenance.web;

import java.util.List;

/** One page of results plus the unpaged total. */
public class PageResult<T> {

    private final List<T> list;
    private final long total;

    public PageResult(List<T> list, long total) {
        this.list = list;
        this.total = total;
    }

    public List<T> getList() {
        return list;
    }

    public long getTotal() {
        return total;
    }
}
