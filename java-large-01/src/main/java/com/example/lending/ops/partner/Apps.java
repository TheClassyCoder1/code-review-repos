package com.example.lending.ops.partner;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Base64;

/** Partner application reachable by SSO; credentials and sharedPassword are stored encrypted. */
@Data
public class Apps {
    private String id, instId, loginUrl, principal, credentials, sharedPassword, extendAttr;
    private int isExtendAttr;
    private byte[] icon;

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class AppsExtendApiDetails extends Apps {
        private String iconBase64;

        public void transIconBase64() {
            iconBase64 = getIcon() == null ? null : Base64.getEncoder().encodeToString(getIcon());
        }
    }
}
