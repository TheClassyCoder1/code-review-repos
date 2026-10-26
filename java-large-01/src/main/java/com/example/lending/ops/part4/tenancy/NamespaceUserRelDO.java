package com.example.lending.ops.part4.tenancy;

import lombok.Data;
import lombok.Setter;
import lombok.experimental.Accessors;

/** Grants an operator access to one partner configuration namespace. */
@Data
public class NamespaceUserRelDO {

    private String id;
    private String namespaceId;
    private String userId;

    @Setter
    @Accessors(fluent = true, chain = true)
    public static final class NamespaceUserRelDOBuilder {
        private String id;
        private String namespaceId;
        private String userId;

        public NamespaceUserRelDO build() {
            NamespaceUserRelDO namespaceUserRelDO = new NamespaceUserRelDO();
            namespaceUserRelDO.setId(id);
            namespaceUserRelDO.setNamespaceId(namespaceId);
            namespaceUserRelDO.setUserId(userId);
            return namespaceUserRelDO;
        }
    }
}
