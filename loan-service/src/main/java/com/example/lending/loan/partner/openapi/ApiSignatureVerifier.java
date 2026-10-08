package com.example.lending.loan.partner.openapi;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
public class ApiSignatureVerifier {

    private static final Logger LOG = LoggerFactory.getLogger(ApiSignatureVerifier.class);

    public boolean verify(String apiKey, Map<String, List<String>> params, String secret, String signature) {
        if (signature == null) {
            return false;
        }
        String expected = generateSignature(apiKey, params, secret);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    }

    private String generateSignature(String apiKey, Map<String, List<String>> params, String secret) {
        Preconditions.checkArgument(apiKey != null && secret != null, "Invalid API key");

        String encryptString = generateEncryptString(params);
        String plainText = String.format("%s%s%s", apiKey, encryptString, secret);
        String signCal = getMD5(plainText);
        LOG.debug(
                "Calculated signature for plain text:{}, calculated signature:{}", plainText, signCal);
        return signCal;
    }

    private static String generateEncryptString(Map<String, List<String>> params) {
        StringBuilder builder = new StringBuilder();
        new TreeMap<>(params).forEach((key, values) -> {
            for (String value : values) {
                builder.append(key).append(value);
            }
        });
        return builder.toString();
    }

    private static String getMD5(String plainText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(plainText.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
