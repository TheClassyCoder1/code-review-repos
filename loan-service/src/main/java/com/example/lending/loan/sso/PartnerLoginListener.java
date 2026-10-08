package com.example.lending.loan.sso;

import org.slf4j.*;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Map.Entry;

@Component
public class PartnerLoginListener {

    private static final Logger partnerAuthLog = LoggerFactory.getLogger(PartnerLoginListener.class);

    @EventListener
    public void onAuthenticated(PartnerIdentity identity) {
        logLogin(identity, identity.partnerId(), identity.displayName(), identity.clientId());
    }

    private void logLogin(PartnerIdentity identity, String partnerId, String displayName, String clientId) {
        partnerAuthLog.debug("partner authentication success, partnerId=[{}] displayName=[{}] clientId=[{}]",
                partnerId, displayName, clientId);
        if (partnerAuthLog.isTraceEnabled()) {
            Map<String, Object> claims = identity.claims();
            for (Entry<String, Object> entry : claims.entrySet()) {
                partnerAuthLog.trace("partner authentication claims [{}={}]", entry.getKey(), entry.getValue());
            }
        }
    }
}
