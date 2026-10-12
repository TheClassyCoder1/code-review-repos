package com.example.lending.loan.servicing.collections.documents;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Read access to identification cards and their scans. */
@Service
@Transactional(readOnly = true)
public class DebtorDocumentService {

    private final JdbcTemplate jdbcTemplate;
    private final IdentityDocumentScanRepository scans;

    public DebtorDocumentService(JdbcTemplate jdbcTemplate, IdentityDocumentScanRepository scans) {
        this.jdbcTemplate = jdbcTemplate;
        this.scans = scans;
    }

    public boolean identificationCardExists(String debtorIdentifier, String number) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lending.servicing_identification_cards WHERE number = ? AND debtor_identifier = ?",
                Integer.class, number, debtorIdentifier);
        return count != null && count > 0;
    }

    public boolean identificationCardScanExists(String number, String scanIdentifier) {
        return scans.existsByCardNumberAndIdentifier(number, scanIdentifier);
    }

    public Optional<IdentityDocumentScan> findScan(String number, String scanIdentifier) {
        return scans.findByCardNumberAndIdentifier(number, scanIdentifier);
    }
}
