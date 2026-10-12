package com.example.lending.loan.servicing.settlement.crypto;

import java.util.Map;

/** JWE content encryption method negotiated with a partner for encrypted settlement files. */
public class JweEncryptionMethod {

    public enum Algorithm { A128GCM, A192GCM, A256GCM, A128CBC_HS256, A256CBC_HS512 }

    private static final Map<String, Algorithm> BY_NAME = Map.of(
            "A128GCM", Algorithm.A128GCM,
            "A192GCM", Algorithm.A192GCM,
            "A256GCM", Algorithm.A256GCM,
            "A128CBC-HS256", Algorithm.A128CBC_HS256,
            "A256CBC-HS512", Algorithm.A256CBC_HS512);

    private String algorithmName;
    private Algorithm algorithm;

    public static JweEncryptionMethod getForAlgorithmName(String algorithmName) {
        JweEncryptionMethod ent = new JweEncryptionMethod();
        ent.setAlgorithmName(algorithmName);
        if (ent.getAlgorithm() == null) {
            return null;
        } else {
            return ent;
        }
    }

    public String getAlgorithmName() {
        return algorithmName;
    }

    public void setAlgorithmName(String algorithmName) {
        this.algorithmName = algorithmName;
        this.algorithm = algorithmName == null ? null : BY_NAME.get(algorithmName);
    }

    public Algorithm getAlgorithm() {
        return algorithm;
    }
}
