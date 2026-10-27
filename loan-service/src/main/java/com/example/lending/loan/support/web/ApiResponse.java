package com.example.lending.loan.support.web;

public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> ofSuccess() {
        return new ApiResponse<>(true, null, null);
    }

    public static <T> ApiResponse<T> ofSuccess(T data) {
        return new ApiResponse<>(true, null, data);
    }

    public static <T> ApiResponse<T> ofFail(String message) {
        return new ApiResponse<>(false, message, null);
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }
}
