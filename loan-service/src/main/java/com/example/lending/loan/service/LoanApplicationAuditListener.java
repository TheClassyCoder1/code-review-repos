package com.example.lending.loan.service;

import com.example.lending.loan.entity.AuditLog;
import com.example.lending.loan.kafka.LoanAppliedEvent;
import com.example.lending.loan.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Writes an audit row for high-value applications so credit ops can sample them for
 * second-line review. Only internal ids go into the audit message.
 */
@Component
public class LoanApplicationAuditListener {

    private static final Logger log = LoggerFactory.getLogger(LoanApplicationAuditListener.class);

    static final String ENTITY_TYPE = "LOAN_APPLICATION";

    private final AuditLogRepository auditLogRepository;

    public LoanApplicationAuditListener(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @EventListener(condition = "#event.getAmount() >= 100000")
    public void onHighValueApplication(LoanAppliedEvent event) {
        AuditLog entry = new AuditLog();
        entry.setEntityType(ENTITY_TYPE);
        entry.setMessage("high-value application loanId=" + event.getLoanId() + " userId=" + event.getUserId());
        auditLogRepository.save(entry);
        log.info("queued loan {} for second-line review", event.getLoanId());
    }
}
