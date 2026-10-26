package com.example.lending.platform.billing.controller;

import com.example.lending.platform.billing.invoice.InvoiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.format.DateTimeParseException;
import java.util.Map;

@RestControllerAdvice(assignableTypes = {InvoiceController.class, CollectionsReportController.class})
public class InvoiceExceptionHandler {

    @ExceptionHandler(InvoiceException.class)
    public ResponseEntity<Map<String, String>> handle(InvoiceException e) {
        HttpStatus status;
        switch (e.getCode()) {
            case NOT_FOUND:
                status = HttpStatus.NOT_FOUND;
                break;
            case INVALID_STATE:
                status = HttpStatus.CONFLICT;
                break;
            case ACCOUNT_UNAVAILABLE:
                status = HttpStatus.SERVICE_UNAVAILABLE;
                break;
            default:
                status = HttpStatus.UNPROCESSABLE_ENTITY;
        }
        return ResponseEntity.status(status).body(Map.of("code", e.getCode().name(), "message", e.getMessage()));
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<Map<String, String>> handleBadMonth(DateTimeParseException e) {
        return ResponseEntity.badRequest().body(Map.of("code", "INVALID_REQUEST", "message", "month must be yyyy-MM"));
    }
}
