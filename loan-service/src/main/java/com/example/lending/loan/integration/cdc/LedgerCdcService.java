package com.example.lending.loan.integration.cdc;

import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Service
public class LedgerCdcService {

    private static final String SLOT_PREFIX = "lending_cdc_partner_";

    private final LedgerCdcConnection connection;

    public LedgerCdcService(DataSource dataSource) {
        this.connection = new LedgerCdcConnection(dataSource);
    }

    public boolean dropPartnerSlot(long partnerId) {
        return connection.dropReplicationSlot(slotNameFor(partnerId));
    }

    static String slotNameFor(long partnerId) {
        return SLOT_PREFIX + partnerId;
    }
}
