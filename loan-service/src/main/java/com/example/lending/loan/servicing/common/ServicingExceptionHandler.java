package com.example.lending.loan.servicing.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps exceptions raised by servicing controllers to the response envelope. */
@RestControllerAdvice(basePackages = "com.example.lending.loan.servicing")
public class ServicingExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ServicingExceptionHandler.class);

    @ExceptionHandler(ServicingException.class)
    public ResponseEntity<ServicingResult<Void>> handleServicingException(ServicingException e) {
        return ResponseEntity.status(e.getStatus()).body(ServicingResult.failed(e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ServicingResult<Void> handleAccessDenied(AccessDeniedException e) {
        return ServicingResult.failed("Access denied");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ServicingResult<Void> handleGlobalException(Exception e) {
        log.error("Unhandled servicing error ex={}", e.getMessage(), e);
        return ServicingResult.failed(e.getLocalizedMessage());
    }
}
