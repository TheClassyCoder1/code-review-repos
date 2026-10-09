package com.example.lending.loan.servicing.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Paging parameters bound from query strings. Size is capped at {@link #MAX_SIZE}. */
public class PageRequestParams {

    public static final int MAX_SIZE = 200;

    private int page = 0;
    private int size = 20;

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(0, page);
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = Math.min(MAX_SIZE, Math.max(1, size));
    }

    public Pageable toPageable(Sort sort) {
        return PageRequest.of(page, size, sort);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PageRequestParams that && page == that.page && size == that.size;
    }

    @Override
    public int hashCode() {
        return 31 * page + size;
    }
}
