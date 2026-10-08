package com.example.lending.loan.partner.apps;

import java.util.Base64;

public class PartnerAppDetails {

    private String id;
    private Long tenantId;
    private String name;
    private String clientId;
    private String clientSecret;
    private byte[] icon;
    private String iconBase64;

    public void transIconBase64() {
        if (icon != null) {
            iconBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(icon);
            icon = null;
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public byte[] getIcon() { return icon; }
    public void setIcon(byte[] icon) { this.icon = icon; }
    public String getIconBase64() { return iconBase64; }
}
