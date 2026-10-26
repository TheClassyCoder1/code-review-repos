package com.example.lending.ops.partner.jwt.token;

import lombok.*;
import java.time.Instant;
import java.util.*;

/** Minimal JOSE model for the short-lived partner SSO login token. */
public interface JWT {
    String serialize();

    static String b64(Object json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.toString().getBytes());
    }

    record JWSAlgorithm(String name, String jcaName) {
        public static final JWSAlgorithm HS256 = new JWSAlgorithm("HS256", "HmacSHA256");
    }

    record JWSHeader(JWSAlgorithm algorithm) {
        public String toString() { return "{\"alg\":\"" + algorithm.name() + "\"}"; }
    }

    record PlainJWT(JWTClaimsSet claims) implements JWT {
        public String serialize() { return b64("{\"alg\":\"none\"}") + "." + b64(claims) + "."; }
    }

    @Getter
    @RequiredArgsConstructor
    class SignedJWT implements JWT {
        private final JWSHeader header;
        private final JWTClaimsSet claims;
        @Setter
        private String signature;
        public String getSigningInput() { return b64(header) + "." + b64(claims); }
        public String serialize() { return getSigningInput() + "." + signature; }
    }

    record JWTClaimsSet(String sub, String iss, String jti, Date exp, Date iat) {
        public String toString() {
            return String.format("{\"sub\":\"%s\",\"iss\":\"%s\",\"jti\":\"%s\",\"exp\":%d,\"iat\":%d}",
                    sub, iss, jti, exp.getTime() / 1000, iat.getTime() / 1000);
        }

        public static class Builder {
            private String sub, iss, jti;
            private Date exp, iat;
            public Builder subject(String v) { sub = v; return this; }
            public Builder issuer(String v) { iss = v; return this; }
            public Builder jwtID(String v) { jti = v; return this; }
            public Builder expirationTime(Date v) { exp = v; return this; }
            public Builder issueTime(Date v) { iat = v; return this; }
            public JWTClaimsSet build() { return new JWTClaimsSet(sub, iss, jti, exp, iat); }
        }
    }

    record DateTime(Instant instant) {
        public static DateTime now() { return new DateTime(Instant.now()); }
        public DateTime plusMinutes(int minutes) { return new DateTime(instant.plusSeconds(minutes * 60L)); }
        public Date toDate() { return Date.from(instant); }
    }
}
