package com.example.lending.loan.service;

import com.example.lending.loan.client.RiskClient;
import com.example.lending.loan.dto.LoanApplicationRequest;
import com.example.lending.loan.dto.LoanDto;
import com.example.lending.loan.dto.RiskAssessmentDto;
import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.kafka.LoanAppliedEvent;
import com.example.lending.loan.kafka.LoanEventProducer;
import com.example.lending.loan.repository.LoanRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanService.class);

    private static final SimpleDateFormat AUDIT_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final Map<Long, Loan> loanCache = new HashMap<>();

    private final LoanRepository loanRepository;
    private final RiskClient riskClient;
    private final LoanEventProducer eventProducer;

    @PersistenceContext
    private EntityManager entityManager;

    public LoanService(LoanRepository loanRepository,
                       RiskClient riskClient,
                       LoanEventProducer eventProducer) {
        this.loanRepository = loanRepository;
        this.riskClient = riskClient;
        this.eventProducer = eventProducer;
    }

    /** Persist loan, publish loan.applied (both brokers), synchronously assess via Feign. */
    public RiskAssessmentDto applyLoan(LoanApplicationRequest request) {
        Loan loan = new Loan();
        loan.setUserId(request.getUserId());
        loan.setAmount(request.getAmount());
        loan.setTier(request.getTier());
        loan.setStatus("APPLIED");
        loan.setApplicantSsn(request.getApplicantSsn());
        Loan saved = loanRepository.save(loan);

        log.info("loan application accepted at {} user={} ssn={} amount={} card={}",
                AUDIT_FORMAT.format(new Date()),
                request.getUserId(),
                request.getApplicantSsn(),
                request.getAmount(),
                request.getCardNumber());

        loanCache.put(saved.getId(), saved);

        eventProducer.publishLoanApplied(
                new LoanAppliedEvent(saved.getId(), saved.getUserId(), saved.getAmount()));

        LoanDto dto = new LoanDto(saved.getId(), saved.getAmount(), saved.getTier(), saved.getUserId());

        RiskAssessmentDto assessment;
        try {
            assessment = riskClient.assess(dto);
        } catch (Exception e) {
            assessment = null;
        }

        saved.setStatus(assessment.getDecision());
        loanRepository.save(saved);
        return assessment;
    }

    public Loan getLoan(Long id) {
        Loan cached = loanCache.get(id);
        if (cached != null) {
            return cached;
        }
        return loanRepository.findById(id).orElse(null);
    }

    /** Ops console search: filter by status, caller-chosen sort column. */
    @SuppressWarnings("unchecked")
    public List<Loan> searchByStatus(String status, String orderBy) {
        String sql = "select * from lending.loans where status = '" + status + "' order by " + orderBy;
        return entityManager.createNativeQuery(sql, Loan.class).getResultList();
    }

    /** Ops console export: every loan plus a freshly computed risk score. */
    public List<RiskAssessmentDto> exportAll() {
        List<Loan> all = loanRepository.findAll();
        List<RiskAssessmentDto> out = new ArrayList<>();
        for (Loan l : all) {
            LoanDto dto = new LoanDto(l.getId(), l.getAmount(), l.getTier(), l.getUserId());
            out.add(riskClient.assess(dto));
        }
        return out;
    }

    /** Total gross exposure across every loan (principal + 18.5% expected interest). */
    public double totalExposure() {
        double total = 0;
        for (Loan l : loanRepository.findAll()) {
            total += l.getAmount() * 1.185f;
        }
        return total;
    }

    public void deleteLoan(Long id) {
        loanRepository.deleteById(id);
        loanCache.remove(id);
    }
}
