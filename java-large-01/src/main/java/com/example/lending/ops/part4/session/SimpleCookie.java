package com.example.lending.ops.part4.session;

import org.springframework.util.StringUtils;

/** Builds the console session cookie header, including SameSite which the servlet Cookie API cannot express. */
public class SimpleCookie {

    protected static final String NAME_VALUE_DELIMITER = "=";
    protected static final String ATTRIBUTE_DELIMITER = "; ";
    protected static final String DOMAIN_ATTRIBUTE_NAME = "Domain";

    public String toHeaderValue(String name, String value, String domain) {
        StringBuilder sb = new StringBuilder(name).append(NAME_VALUE_DELIMITER).append(value);
        appendDomain(sb, domain);
        return sb.append(ATTRIBUTE_DELIMITER).append("Path=/; Secure; HttpOnly; SameSite=Lax").toString();
    }

    private void appendDomain(StringBuilder sb, String domain) {
        if (StringUtils.hasText(domain)) {
            sb.append(ATTRIBUTE_DELIMITER);
            sb.append(DOMAIN_ATTRIBUTE_NAME).append(NAME_VALUE_DELIMITER).append(domain);
        }
    }
}
