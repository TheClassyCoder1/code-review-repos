package com.example.lending.ops.part3.gateway;

import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.security.cert.*;
import java.util.*;

/** Verifies repayment notifications from the card gateway against certificates served by its regional endpoints. */
@Component
public class PaymentGatewayCallbackVerifier {
    private static final String CERTIFICATE_TYPE = "X.509";
    enum Region { EU_WEST, AP_SOUTH; String getEndpoint() { return "certs." + name().toLowerCase().replace('_', '-') + ".paygate.example"; } }
    public boolean verify(String payload, String base64Signature, String signCertUrl) throws Exception {
        X509Certificate certificate = getCertificate(signCertUrl);
        certificate.checkValidity();
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(certificate.getPublicKey());
        signature.update(payload.getBytes(StandardCharsets.UTF_8));
        return signature.verify(Base64.getDecoder().decode(base64Signature));
    }
    private X509Certificate getCertificate(String signCertUrl) throws Exception {
        URL url = new URL(signCertUrl);
        if (!"https".equalsIgnoreCase(url.getProtocol())) {
            throw new SecurityException("Only HTTPS is allowed");
        }
        boolean trusted = Arrays.stream(Region.values()).anyMatch(ep -> ep.getEndpoint().equals(url.getHost()));
        if (!trusted) {
            throw new SecurityException("Untrusted domain: " + url.getHost());
        }
        try (InputStream in = url.openStream()) {
            CertificateFactory cf = CertificateFactory.getInstance(CERTIFICATE_TYPE);
            return (X509Certificate) cf.generateCertificate(in);
        }
    }
}
