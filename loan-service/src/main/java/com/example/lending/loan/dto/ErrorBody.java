package com.example.lending.loan.dto;

public record ErrorBody(int code, String msg) {

    public static ErrorBody error(String msg) {
        return new ErrorBody(1, msg);
    }
}
