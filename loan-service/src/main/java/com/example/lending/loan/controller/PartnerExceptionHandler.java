package com.example.lending.loan.controller;

import com.example.lending.loan.dto.ErrorBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = {PartnerAdminController.class, DocumentController.class})
public class PartnerExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PartnerExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorBody> handleStatus(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(ErrorBody.error(e.getReason()));
    }

    @ExceptionHandler
    public ErrorBody handlerException(Exception e) {
        log.error("Partner request failed", e);
        return ErrorBody.error(e.getMessage());
    }
}
