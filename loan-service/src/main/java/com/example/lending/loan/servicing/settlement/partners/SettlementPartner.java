package com.example.lending.loan.servicing.settlement.partners;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Basic;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Bank or payment processor that settles loan repayments with us. */
@Entity
@Table(name = "settlement_partners", schema = "lending")
public class SettlementPartner {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "client_id", nullable = false)
    private String clientId;
    @Column(name = "client_secret", nullable = false)
    private String clientSecret;
    @Column(name = "callback_url")
    private String callbackUrl;
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "logo")
    private byte[] logo;
    @Transient
    private String logoBase64;
    @Version
    @Column(name = "version", nullable = false)
    private long version;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "settlement_partner_api_keys", schema = "lending",
            joinColumns = @JoinColumn(name = "partner_id"))
    private List<PartnerApiKey> apiKeys = new ArrayList<>();

    public void transLogoBase64() {
        this.logoBase64 = logo == null ? null : "data:image/png;base64," + Base64.getEncoder().encodeToString(logo);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public String getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(String callbackUrl) { this.callbackUrl = callbackUrl; }

    @JsonIgnore
    public byte[] getLogo() { return logo; }
    public void setLogo(byte[] logo) { this.logo = logo; }
    public String getLogoBase64() { return logoBase64; }

    public long getVersion() { return version; }
    public List<PartnerApiKey> getApiKeys() { return apiKeys; }
    public void setApiKeys(List<PartnerApiKey> apiKeys) { this.apiKeys = apiKeys; }
}
