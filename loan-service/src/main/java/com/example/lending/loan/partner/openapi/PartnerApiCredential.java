package com.example.lending.loan.partner.openapi;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "partner_api_credentials", schema = "lending")
public class PartnerApiCredential {

    @Id
    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "secret")
    private String secret;

    @Column(name = "tenant_id")
    private Long tenantId;

    public String getApiKey() { return apiKey; }
    public String getSecret() { return secret; }
    public Long getTenantId() { return tenantId; }
}
