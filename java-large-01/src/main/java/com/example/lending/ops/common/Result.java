package com.example.lending.ops.common;

import lombok.Data;

/** Response envelopes used by the different console modules. */
@Data
public class Result<T> {
    private boolean success = true;
    private String message = "";
    private Integer code = 200;
    private T result;

    public static <T> Result<T> OK(T data) {
        Result<T> r = new Result<>();
        r.setResult(data);
        return r;
    }

    public static <T> Result<T> error(String msg) { return new Result<T>().error500(msg); }

    public Result<T> error500(String message) {
        this.message = message;
        this.code = 500;
        this.success = false;
        return this;
    }

    public record SaResult(int code, String msg, Object data) {
        public static SaResult ok(String msg) { return new SaResult(200, msg, null); }
        public static SaResult error(String msg) { return new SaResult(500, msg, null); }
        public static SaResult data(Object data) { return new SaResult(200, "ok", data); }
    }

    public record CommonResult<T>(long code, String message, T data) {
        public static <T> CommonResult<T> success(T data) { return new CommonResult<>(200, "ok", data); }
        public static <T> CommonResult<T> failed() { return new CommonResult<>(500, "operation failed", null); }
    }

    public record Response<T>(int code, String msg, T data) {
        public static <T> Response<T> success(T data) { return new Response<>(200, null, data); }
        public static <T> Response<T> ofSuccess() { return success(null); }
        public static <T> Response<T> ofFail(String msg) { return new Response<>(500, msg, null); }
        public T getData() { return data; }
    }

    public record Message<T>(int code, String message, T data) {
        public Message(T data) { this(0, "", data); }
    }
}
