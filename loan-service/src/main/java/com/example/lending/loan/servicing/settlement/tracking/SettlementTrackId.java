package com.example.lending.loan.servicing.settlement.tracking;

import java.util.Map;

/** Identity of a tracked settlement run on the gateway. */
public class SettlementTrackId {

    public enum SettlementMode { REAL_TIME, BATCH, MANUAL }

    private final SettlementMode executeMode;
    private final String clusterId;
    private final String namespace;
    private final String partnerId;
    private final String batchId;
    private final Long groupId;
    private final Map<String, String> properties;

    public SettlementTrackId(SettlementMode executeMode, String clusterId, String namespace, String partnerId,
                             String batchId, Long groupId, Map<String, String> properties) {
        this.executeMode = executeMode;
        this.clusterId = clusterId;
        this.namespace = namespace;
        this.partnerId = partnerId;
        this.batchId = batchId;
        this.groupId = groupId;
        this.properties = properties == null ? Map.of() : Map.copyOf(properties);
    }

    public SettlementMode getExecuteMode() { return executeMode; }
    public String getClusterId() { return clusterId; }

    public String getNamespace() { return namespace; }
    public String getPartnerId() { return partnerId; }

    public String getBatchId() { return batchId; }
    public Long getGroupId() { return groupId; }

    public Map<String, String> getProperties() { return properties; }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SettlementTrackId)) {
            return false;
        }
        SettlementTrackId that = (SettlementTrackId) obj;
        return executeMode == that.executeMode
            && java.util.Objects.equals(clusterId, that.clusterId)
            && java.util.Objects.equals(namespace, that.namespace)
            && java.util.Objects.equals(partnerId, that.partnerId)
            && java.util.Objects.equals(batchId, that.batchId)
            && java.util.Objects.equals(groupId, that.groupId)
            && java.util.Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(executeMode, clusterId, namespace, partnerId, batchId, groupId, properties);
    }
}
