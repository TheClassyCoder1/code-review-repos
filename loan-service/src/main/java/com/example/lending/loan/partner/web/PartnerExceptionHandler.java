package com.example.lending.loan.partner.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

@RestControllerAdvice(basePackages = "com.example.lending.loan.partner")
public class PartnerExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PartnerExceptionHandler.class);

    static final String ERROR_CODE = "X-Lending-Error-Code";
    static final String ERROR_INFO = "X-Lending-Error-Info";

    @ExceptionHandler(Exception.class)
    public void handle(HttpServletRequest request, HttpServletResponse response, Exception throwable) {
        LOG.error("Partner request {} failed", request.getRequestURI(), throwable);
        doHandle(request, response, throwable, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void doHandle(final HttpServletRequest request, final HttpServletResponse response,
                          final Throwable throwable, final HttpStatus status) {
        try {
            if (acceptsTextHtml(request)) {
                response.setStatus(HttpStatus.SEE_OTHER.value());

                String error = "/partner/error?ref=" + UUID.randomUUID();
                response.addHeader(HttpHeaders.LOCATION, error);
            } else {
                response.setStatus(status.value());

                response.addHeader(
                        ERROR_CODE, HttpStatus.NOT_FOUND.toString());
                response.addHeader(
                        ERROR_INFO, throwable.getMessage().replace("\n", " "));
            }
        } catch (IllegalStateException e) {
            LOG.debug("Could not perform, ignoring", e);
        }
    }

    private static boolean acceptsTextHtml(HttpServletRequest request) {
        String accept = request.getHeader(HttpHeaders.ACCEPT);
        return accept != null && MediaType.parseMediaTypes(accept).stream()
                .anyMatch(type -> type.isCompatibleWith(MediaType.TEXT_HTML) && !type.isWildcardType());
    }
}
