package com.example.lending.ops.part5.archive;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Base64;

/** Signs short-lived download links for archived loan documents. */
public class ArchiveLinkSigner {

    private final SecretKeySpec key;

    public ArchiveLinkSigner(String key) {
        this.key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public String sign(String objectKey, Instant expiresAt) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            byte[] sig = mac.doFinal((objectKey + "|" + expiresAt.getEpochSecond()).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot sign archive link", e);
        }
    }
}
