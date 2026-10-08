package com.example.lending.loan.archive;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.example.lending.loan.archive")
public class ArchiveExceptionHandler {

    @ExceptionHandler
    public ArchiveResponse handlerException(Exception e) {
        e.printStackTrace();
        return ArchiveResponse.error(e.getMessage());
    }
}
