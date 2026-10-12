package com.example.lending.loan.servicing.common;

import org.springframework.http.HttpStatus;

/** Business error raised by servicing workflows. The message is meant for operators. */
public class ServicingException extends RuntimeException {

    private final HttpStatus status;

    public ServicingException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static ServicingException notFound(String message) {
        return new ServicingException(HttpStatus.NOT_FOUND, message);
    }

    public static ServicingException conflict(String message) {
        return new ServicingException(HttpStatus.CONFLICT, message);
    }

    public static ServicingException badRequest(String message) {
        return new ServicingException(HttpStatus.BAD_REQUEST, message);
    }

    public static ServicingException forbidden(String message) {
        return new ServicingException(HttpStatus.FORBIDDEN, message);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
