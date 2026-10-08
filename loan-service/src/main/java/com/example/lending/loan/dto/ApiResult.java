package com.example.lending.loan.dto;

public record ApiResult(int code, String msg, Object data) {

    public static ApiResult success(Object data) {
        return new ApiResult(0, "ok", data);
    }

    public static ApiResult error(String msg) {
        return new ApiResult(1, msg, null);
    }
}
