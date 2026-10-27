package com.example.lending.ops.partner.adapter;

import lombok.Setter;
import lombok.Value;
import org.springframework.web.servlet.ModelAndView;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

/** Builds the browser hand-off for single sign-on into a partner application. */
@Setter
public abstract class AbstractAuthorizeAdapter {
    protected Object app;
    protected UserInfo userInfo;

    public abstract ModelAndView authorize(ModelAndView modelAndView);

    @Value
    public static class UserInfo { String id, email; }

    @Value
    public static class ExtraAttr { String attr, value; }

    /** Extra query attributes configured on a partner app, stored as {@code k1=v1&k2=v2}. */
    public record ExtraAttrs(String raw) {
        public List<ExtraAttr> getExtraAttrs() {
            return Arrays.stream(raw.split("&")).map(p -> p.split("=", 2)).map(kv -> new ExtraAttr(kv[0], kv[kv.length - 1])).toList();
        }
    }

    /** SHA1withRSA, hex encoded, as the partner mail suite's login endpoint requires. */
    public static class NeteaseRSATool {
        public String generateSHA1withRSASigature(String src, String privateKeyBase64) {
            try {
                Signature signature = Signature.getInstance("SHA1withRSA");
                signature.initSign(KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyBase64))));
                signature.update(src.getBytes());
                return HexFormat.of().formatHex(signature.sign());
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("unable to sign partner sso request", e);
            }
        }
    }
}
