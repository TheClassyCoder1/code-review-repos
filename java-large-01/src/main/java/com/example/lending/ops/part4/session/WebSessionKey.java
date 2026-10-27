package com.example.lending.ops.part4.session;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

/** Session lookup key that also carries the current request/response so cookies can be refreshed. */
public class WebSessionKey {

    private final ServletRequest servletRequest;
    private final ServletResponse servletResponse;

    public WebSessionKey(ServletRequest request, ServletResponse response) {
        if (request == null) {
            throw new NullPointerException("request argument cannot be null.");
        }
        if (response == null) {
            throw new NullPointerException("response argument cannot be null.");
        }
        this.servletRequest = request;
        this.servletResponse = response;
    }

    public ServletRequest getServletRequest() { return servletRequest; }

    public ServletResponse getServletResponse() { return servletResponse; }
}
