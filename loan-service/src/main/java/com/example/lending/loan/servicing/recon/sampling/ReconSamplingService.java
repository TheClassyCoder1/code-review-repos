package com.example.lending.loan.servicing.recon.sampling;

import com.example.lending.loan.servicing.common.ServicingException;
import com.example.lending.loan.servicing.recon.sampling.ReconSampleQueries.TableId;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Returns small samples of reconciliation data for the workbench. */
@Service
public class ReconSamplingService {

    /** Tables that can be sampled from the reconciliation workbench. */
    enum ReconSource {

        BANK_TRANSACTIONS(new TableId("lending", "recon_bank_transactions")),
        LEDGER_ENTRIES(new TableId("lending", "recon_ledger_entries"));

        private final TableId tableId;

        ReconSource(TableId tableId) {
            this.tableId = tableId;
        }

        TableId tableId() {
            return tableId;
        }
    }

    /** Predefined sampling queries. */
    enum SampleProfile {

        UNMATCHED("id, value_date, amount, currency, match_status", "match_status = 'UNMATCHED'", "value_date DESC"),
        LARGE_AMOUNTS("id, value_date, amount, currency, match_status", "amount >= 100000", "amount DESC"),
        LATEST("id, value_date, amount, currency, match_status", null, "created_at DESC");

        private final String projection;
        private final String condition;
        private final String orderBy;

        SampleProfile(String projection, String condition, String orderBy) {
            this.projection = projection;
            this.condition = condition;
            this.orderBy = orderBy;
        }
    }

    private static final int MAX_ROWS = 500;

    private final JdbcTemplate jdbcTemplate;

    public ReconSamplingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> sample(String source, String profile, int limit) {
        ReconSource reconSource;
        SampleProfile sampleProfile;
        try {
            reconSource = ReconSource.valueOf(source.toUpperCase(Locale.ROOT));
            sampleProfile = SampleProfile.valueOf(profile.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ServicingException.badRequest("Unknown source or profile");
        }
        int boundedLimit = Math.min(Math.max(limit, 1), MAX_ROWS);
        String sql = ReconSampleQueries.selectSample(reconSource.tableId(), boundedLimit,
                sampleProfile.projection,
                Optional.ofNullable(sampleProfile.condition),
                Optional.ofNullable(sampleProfile.orderBy));
        return jdbcTemplate.queryForList(sql);
    }
}
