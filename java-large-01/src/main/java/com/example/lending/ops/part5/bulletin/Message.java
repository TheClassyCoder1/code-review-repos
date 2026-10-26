package com.example.lending.ops.part5.bulletin;

/** Response envelope for the bulletin board endpoints. */
public record Message<T>(int code, String msg, T data) {

    public static <T> Message<T> success(String msg) {
        return new Message<>(0, msg, null);
    }
}
