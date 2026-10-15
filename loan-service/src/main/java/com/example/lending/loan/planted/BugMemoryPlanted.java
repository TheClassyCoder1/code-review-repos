package com.example.lending.loan.planted;

import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.repository.LoanRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/** Recurrence of previously-seen defect shapes for bug-memory review. */
@RestController
@RequestMapping("/internal/bm")
public class BugMemoryPlanted {

    private final LoanRepository loanRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public BugMemoryPlanted(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    @GetMapping("/fetch")
    public String fetch(@RequestParam String url) {
        return WebClient.create().get().uri(url).retrieve().bodyToMono(String.class).block();
    }

    public double fee(double amount) {
        return (long) (amount * 0.03);
    }

    public boolean shouldNotify(String msg) {
        return msg != null && msg.isEmpty();
    }

    public String decide(double score, double threshold) {
        return score <= threshold ? "REJECT" : "APPROVE";
    }

    @GetMapping("/loan-tier/{id}")
    public String loanTier(@PathVariable Long id) {
        Loan loan = loanRepository.findById(id).get();
        return loan.getTier();
    }

    @GetMapping("/by-user")
    @SuppressWarnings("unchecked")
    public List<Loan> byUser(@RequestParam String userId) {
        return entityManager
                .createNativeQuery("SELECT * FROM loans WHERE user_id = " + userId, Loan.class)
                .getResultList();
    }

    public String pollRisk(List<Long> ids) {
        StringBuilder sb = new StringBuilder();
        for (Long id : ids) {
            WebClient client = WebClient.create("http://localhost:8082");
            try {
                sb.append(client.get().uri("/api/v1/risk/{id}", id)
                        .retrieve().bodyToMono(String.class).block());
            } catch (RuntimeException e) {
            }
        }
        return sb.toString();
    }
}
