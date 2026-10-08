package com.example.lending.loan.partner.sso;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@ConfigurationProperties(prefix = "partner.oidc")
public class OidcProperties {

    private URI authUrl;
    private URI tokenUrl;
    private URI userInfoUrl;
    private String clientId;
    private String clientSecret;
    private URI callbackUrl;
    private URI baseUrl;
    private String groupsClaimName = "groups";
    private String adminGroup;
    private String allowGroup;

    public URI getAuthUrl() { return authUrl; }
    public void setAuthUrl(URI authUrl) { this.authUrl = authUrl; }
    public URI getTokenUrl() { return tokenUrl; }
    public void setTokenUrl(URI tokenUrl) { this.tokenUrl = tokenUrl; }
    public URI getUserInfoUrl() { return userInfoUrl; }
    public void setUserInfoUrl(URI userInfoUrl) { this.userInfoUrl = userInfoUrl; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public URI getCallbackUrl() { return callbackUrl; }
    public void setCallbackUrl(URI callbackUrl) { this.callbackUrl = callbackUrl; }
    public URI getBaseUrl() { return baseUrl; }
    public void setBaseUrl(URI baseUrl) { this.baseUrl = baseUrl; }
    public String getGroupsClaimName() { return groupsClaimName; }
    public void setGroupsClaimName(String groupsClaimName) { this.groupsClaimName = groupsClaimName; }
    public String getAdminGroup() { return adminGroup; }
    public void setAdminGroup(String adminGroup) { this.adminGroup = adminGroup; }
    public String getAllowGroup() { return allowGroup; }
    public void setAllowGroup(String allowGroup) { this.allowGroup = allowGroup; }
}
