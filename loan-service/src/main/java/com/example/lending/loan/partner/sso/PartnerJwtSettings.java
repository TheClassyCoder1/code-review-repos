package com.example.lending.loan.partner.sso;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "partner_jwt_settings", schema = "lending")
public class PartnerJwtSettings {

    @Id
    @Column(name = "app_id")
    private String appId;

    @Column(name = "signature")
    private String signature;

    @Column(name = "signature_key")
    private String signatureKey;

    @Column(name = "algorithm")
    private String algorithm;

    @Column(name = "algorithm_key")
    private String algorithmKey;

    public String getAppId() { return appId; }
    public String getSignature() { return signature; }
    public String getSignatureKey() { return signatureKey; }
    public String getAlgorithm() { return algorithm; }
    public String getAlgorithmKey() { return algorithmKey; }
}
