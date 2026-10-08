package com.example.lending.loan.webhook;

import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.net.*;

public final class CallbackUrlValidator {

    private CallbackUrlValidator() {
    }

    public static void checkCallbackUrl(String callbackUrl) {
        if (!StringUtils.hasText(callbackUrl)) {
            throw invalid("Callback URL is empty");
        }
        URI uri;
        try {
            uri = new URI(callbackUrl);
        } catch (URISyntaxException e) {
            throw invalid("Callback URL is malformed");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw invalid("Only http and https callback URLs are allowed");
        }
        String host = uri.getHost();
        if (!StringUtils.hasText(host)) {
            throw invalid("Callback URL host is empty");
        }
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        try {
            for (InetAddress addr : InetAddress.getAllByName(host)) {
                if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()) {
                    throw invalid("Callback URL must not point to a local address " + addr.getHostAddress());
                }
            }
        } catch (UnknownHostException e) {
            throw invalid("Callback URL host cannot be resolved");
        }
    }

    private static ResponseStatusException invalid(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
