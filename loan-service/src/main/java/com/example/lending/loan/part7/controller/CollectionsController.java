package com.example.lending.loan.part7.controller;

import com.example.lending.loan.part7.dto.OverdueInstallmentRow;
import jakarta.persistence.EntityManager;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Collections work queue: open installments past their due date, oldest first.
 */
@RestController
@RequestMapping("/api/v1/collections")
public class CollectionsController {

    private static final int DEFAULT_LIMIT = 200;

    private final EntityManager entityManager;

    public CollectionsController(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @GetMapping("/overdue")
    public List<OverdueInstallmentRow> overdue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
            @RequestParam(defaultValue = "1") int minDaysPastDue,
            @RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit) {
        LocalDate today = asOf != null ? asOf : LocalDate.now();
        LocalDate cutoff = today.minusDays(Math.max(0, minDaysPastDue - 1L));

        List<Object[]> rows = entityManager.createQuery(
                        "select i.loanId, i.sequenceNo, i.dueDate, i.principal + i.interest "
                                + "from RepaymentInstallment i "
                                + "where i.status = 'DUE' and i.dueDate < :cutoff "
                                + "order by i.dueDate asc, i.loanId asc", Object[].class)
                .setParameter("cutoff", cutoff)
                .setMaxResults(Math.min(limit, 1000))
                .getResultList();

        return rows.stream()
                .map(r -> new OverdueInstallmentRow(
                        (Long) r[0],
                        (Integer) r[1],
                        (LocalDate) r[2],
                        (BigDecimal) r[3],
                        ChronoUnit.DAYS.between((LocalDate) r[2], today)))
                .toList();
    }

    @GetMapping("/overdue/summary")
    public OverdueSummary summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        LocalDate today = asOf != null ? asOf : LocalDate.now();
        Object[] totals = entityManager.createQuery(
                        "select count(distinct i.loanId), count(i), coalesce(sum(i.principal + i.interest), 0) "
                                + "from RepaymentInstallment i where i.status = 'DUE' and i.dueDate < :today",
                        Object[].class)
                .setParameter("today", today)
                .getSingleResult();
        return new OverdueSummary(today, (Long) totals[0], (Long) totals[1], (BigDecimal) totals[2]);
    }

    public record OverdueSummary(LocalDate asOf, long loans, long installments, BigDecimal amountDue) {
    }
}
