package com.example.lending.loan.ops.facility;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory ledger of credit facilities and pending drawdowns for the funding desk. */
@Component
public class FacilityLedger {

    public static class CreditFacility {
        private final String id;
        private final String name;
        private int stock;

        public CreditFacility(String id, String name, int stock) {
            this.id = id;
            this.name = name;
            this.stock = stock;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public int getStock() { return stock; }
        public void setStock(int stock) { this.stock = stock; }
    }

    public static class Drawdown {
        private final String id;
        private final String facilityId;
        private final int quantity;
        private volatile String status = "pending";

        public Drawdown(String id, String facilityId, int quantity) {
            this.id = id;
            this.facilityId = facilityId;
            this.quantity = quantity;
        }

        public String getId() { return id; }
        public String getFacilityId() { return facilityId; }
        public int getQuantity() { return quantity; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    final Map<String, CreditFacility> facilityStore = new ConcurrentHashMap<>();
    final Map<String, Drawdown> drawdownStore = new ConcurrentHashMap<>();

    public void addFacility(CreditFacility facility) {
        facilityStore.put(facility.getId(), facility);
    }

    public void addDrawdown(Drawdown drawdown) {
        drawdownStore.put(drawdown.getId(), drawdown);
    }
}
