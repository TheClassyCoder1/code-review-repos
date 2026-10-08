package com.example.lending.loan.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/** Builds the single sign-on link that drops a borrower straight into the document portal. */
@Component
public class PortalLinkBuilder {

    private static final Logger log = LoggerFactory.getLogger(PortalLinkBuilder.class);

    private static final String LOGIN_URL_TEMPLATE = "sso/login?account=%s&code=%s&time=%s&token=%s";
    private static final String LOGIN_URL_M_TEMPLATE = "m=sso&f=login&account=%s&code=%s&time=%s&token=%s";

    public record PortalApp(String principal, String credentials, String loginUrl, String extendAttr) {
    }

    public String authorize(PortalApp details, String account) {
        String extraAttrs = null;
        if (details.extendAttr() != null && !details.extendAttr().isBlank()) {
            extraAttrs = details.extendAttr();
        }
        log.trace("Extra Attrs " + extraAttrs);
        String code = details.principal();
        String key = details.credentials();
        String time = "" + Instant.now().getEpochSecond();

        String token = md5Hex(code + key + time);

        log.debug("" + token);

        String redirect_uri = details.loginUrl();
        if (redirect_uri.indexOf("index.php?") < 0) {
            if (redirect_uri.endsWith("/")) {
                redirect_uri += String.format(LOGIN_URL_TEMPLATE, account, code, time, token);
            } else {
                redirect_uri += "/" + String.format(LOGIN_URL_TEMPLATE, account, code, time, token);
            }
        } else if (redirect_uri.endsWith("&")) {
            redirect_uri += String.format(LOGIN_URL_M_TEMPLATE, account, code, time, token);
        } else {
            redirect_uri += "&" + String.format(LOGIN_URL_M_TEMPLATE, account, code, time, token);
        }

        log.debug("redirect_uri : " + redirect_uri);
        return redirect_uri;
    }

    private static String md5Hex(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
