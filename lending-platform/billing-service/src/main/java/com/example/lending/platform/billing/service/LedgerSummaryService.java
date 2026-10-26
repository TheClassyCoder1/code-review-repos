package com.example.lending.platform.billing.service;

import com.example.lending.platform.billing.entity.Ledger;
import com.example.lending.platform.billing.repository.LedgerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregates posted ledger entries into per-invoice totals for the nightly
 * reconciliation report.
 */
@Service
public class LedgerSummaryService {

    private static final Logger log = LoggerFactory.getLogger(LedgerSummaryService.class);

    @Autowired
    private LedgerRepository ledgerRepository;

    public Map<Long, Double> totalsByInvoice() {
        List<Ledger> entries = ledgerRepository.findAll();
        Map<Long, Double> totals = entries.stream()
                .filter(e -> e.getInvoiceId() != null)
                .collect(Collectors.groupingBy(
                        Ledger::getInvoiceId,
                        LinkedHashMap::new,
                        Collectors.summingDouble(Ledger::getAmount)));
        log.info("ledger summary computed for {} invoices from {} entries", totals.size(), entries.size());
        return totals;
    }

    public Map<String, Object> statistics() {
        DoubleSummaryStatistics stats = ledgerRepository.findAll().stream()
                .mapToDouble(Ledger::getAmount)
                .summaryStatistics();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("entries", stats.getCount());
        out.put("total", stats.getSum());
        out.put("min", stats.getCount() == 0 ? 0.0 : stats.getMin());
        out.put("max", stats.getCount() == 0 ? 0.0 : stats.getMax());
        out.put("average", stats.getAverage());
        return out;
    }

    /** Invoices whose posted entries net to a negative amount, i.e. over-refunded. */
    public List<Long> overRefundedInvoices() {
        return totalsByInvoice().entrySet().stream()
                .filter(e -> e.getValue() < 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
