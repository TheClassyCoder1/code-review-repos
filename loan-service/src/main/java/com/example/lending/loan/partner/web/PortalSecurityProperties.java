package com.example.lending.loan.partner.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "partner.portal.security")
public class PortalSecurityProperties {

    private boolean forwardedHeaders;
    private boolean csrf = true;
    private Map<String, String> securityHeaders = new LinkedHashMap<>();

    public boolean isForwardedHeaders() { return forwardedHeaders; }
    public void setForwardedHeaders(boolean forwardedHeaders) { this.forwardedHeaders = forwardedHeaders; }
    public boolean isCsrf() { return csrf; }
    public void setCsrf(boolean csrf) { this.csrf = csrf; }
    public Map<String, String> getSecurityHeaders() { return securityHeaders; }
    public void setSecurityHeaders(Map<String, String> securityHeaders) { this.securityHeaders = securityHeaders; }
}
