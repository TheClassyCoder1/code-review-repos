package com.example.lending.loan.bureau;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class BureauSession {

    private final BureauGatewayClient client;

    public BureauSession(BureauGatewayClient client) {
        this.client = client;
    }

    @PreDestroy
    public void disconnect() {
        try {
            client.disconnect()
                    .get();
        } catch (Exception e) {
            throw new IllegalStateException("Could not disconnect from bureau gateway: "
                    + client.getUrl() + ", " + e.getMessage(), e);
        }
    }
}
