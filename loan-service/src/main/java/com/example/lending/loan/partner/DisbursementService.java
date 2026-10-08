package com.example.lending.loan.partner;

import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.repository.LoanRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.util.Set;

@Service
public class DisbursementService {

    private static final Logger log = LoggerFactory.getLogger(DisbursementService.class);
    private static final Set<String> AWAITING_FUNDS = Set.of("APPROVED", "DISBURSING");

    private final LoanRepository loanRepository;
    private final ProcessedCallbackRepository processedCallbackRepository;
    private final Clock clock;

    public DisbursementService(LoanRepository loanRepository,
                               ProcessedCallbackRepository processedCallbackRepository,
                               Clock clock) {
        this.loanRepository = loanRepository;
        this.processedCallbackRepository = processedCallbackRepository;
        this.clock = clock;
    }

    @Transactional
    public void applyCallback(DisbursementCallback callback) {
        if (callback.eventId() == null || callback.loanId() == null || callback.status() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "event_id, loan_id and status are required");
        }
        if (processedCallbackRepository.existsById(callback.eventId())) {
            return;
        }
        Loan loan = loanRepository.findById(callback.loanId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));

        if (AWAITING_FUNDS.contains(loan.getStatus())) {
            switch (callback.status()) {
                case "SETTLED" -> loan.setStatus("DISBURSED");
                case "RETURNED" -> loan.setStatus("DISBURSEMENT_FAILED");
                default -> log.warn("Ignoring unknown disbursement status for loan {}", loan.getId());
            }
        } else {
            log.warn("Disbursement callback for loan {} in status {}", loan.getId(), loan.getStatus());
        }
        processedCallbackRepository.save(new ProcessedCallback(callback.eventId(), clock.instant()));
    }

    public record DisbursementCallback(@JsonProperty("event_id") String eventId,
                                       @JsonProperty("loan_id") Long loanId,
                                       @JsonProperty("status") String status) {
    }
}
