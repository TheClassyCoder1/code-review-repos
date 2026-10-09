package com.example.lending.loan.servicing.common;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Response envelope shared by the servicing back-office endpoints. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServicingResult<T> {

    public static final int OK = 0;
    public static final int FAILED = 1;

    private final int code;
    private final String message;
    private final T data;

    private ServicingResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ServicingResult<T> ok() {
        return new ServicingResult<>(OK, "ok", null);
    }

    public static <T> ServicingResult<T> ok(T data) {
        return new ServicingResult<>(OK, "ok", data);
    }

    public static <T> ServicingResult<T> failed() {
        return new ServicingResult<>(FAILED, "failed", null);
    }

    public static <T> ServicingResult<T> failed(String message) {
        return new ServicingResult<>(FAILED, message, null);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public boolean isOk() {
        return code == OK;
    }
}
