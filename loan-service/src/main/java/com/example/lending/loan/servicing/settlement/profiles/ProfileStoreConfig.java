package com.example.lending.loan.servicing.settlement.profiles;

import java.util.Objects;

/** Connection settings of the remote store that publishes settlement profiles to partner agents. */
public class ProfileStoreConfig {

    private boolean enabled;
    private String endpoint;
    private String namespace;
    private String accessKey;
    private String secretKey;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }
    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (Objects.isNull(o) || getClass() != o.getClass()) {
            return false;
        }
        ProfileStoreConfig that = (ProfileStoreConfig) o;
        return enabled == that.enabled
                && Objects.equals(endpoint, that.endpoint)
                && Objects.equals(namespace, that.namespace)
                && Objects.equals(accessKey, that.accessKey)
                && Objects.equals(secretKey, that.secretKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, endpoint, namespace, accessKey, secretKey);
    }

    @Override
    public String toString() {
        return "ProfileStoreConfig[enabled=" + enabled + ", endpoint=" + endpoint + ", namespace=" + namespace + "]";
    }
}
