package com.acme.warehouse.model;

import java.util.List;

public record PageResponse<T>(List<T> items, boolean hasMore) {

    public static <T> PageResponse<T> of(List<T> rows, int limit) {
        boolean hasMore = rows.size() > limit;
        return new PageResponse<>(hasMore ? rows.subList(0, limit) : rows, hasMore);
    }
}
