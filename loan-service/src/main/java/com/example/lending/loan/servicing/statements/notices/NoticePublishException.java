package com.example.lending.loan.servicing.statements.notices;

/** Raised when a statement notice cannot be stored. */
public class NoticePublishException extends Exception {

    private final int code;

    public NoticePublishException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
