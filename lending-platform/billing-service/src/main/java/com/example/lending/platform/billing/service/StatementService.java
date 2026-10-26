package com.example.lending.platform.billing.service;

import com.example.lending.platform.billing.dto.InvoiceView;
import com.example.lending.platform.billing.dto.StatementView;
import com.example.lending.platform.billing.entity.Invoice;
import com.example.lending.platform.billing.entity.InvoiceStatus;
import com.example.lending.platform.billing.invoice.InvoiceException;
import com.example.lending.platform.billing.repository.InvoiceRepository;
import com.example.lending.platform.common.util.MoneyUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds the monthly statement shown in the borrower portal. The opening balance is everything
 * still unpaid from invoices issued before the period; voided invoices never count.
 */
@Service
public class StatementService {

    private final InvoiceRepository invoiceRepository;

    public StatementService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(readOnly = true)
    public StatementView monthly(Long accountId, YearMonth month) {
        if (accountId == null) {
            throw InvoiceException.invalid("accountId is required");
        }
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        List<Invoice> all = invoiceRepository.findByAccountIdOrderByIssueDateDesc(accountId);
        List<Invoice> inPeriod = invoiceRepository.findByAccountIdAndIssueDateBetweenOrderByIssueDateAsc(accountId, start, end)
                .stream()
                .filter(i -> i.getStatus() != InvoiceStatus.VOID && i.getStatus() != InvoiceStatus.DRAFT)
                .collect(Collectors.toList());

        double opening = all.stream()
                .filter(i -> i.getIssueDate() != null && i.getIssueDate().isBefore(start))
                .filter(i -> i.getStatus().isOpen())
                .mapToDouble(Invoice::outstandingAmount)
                .sum();
        double invoiced = inPeriod.stream().mapToDouble(Invoice::getTotalAmount).sum();
        double paid = inPeriod.stream().mapToDouble(Invoice::getPaidAmount).sum();
        double lateFees = inPeriod.stream().mapToDouble(Invoice::getLateFeeAmount).sum();
        int overdue = (int) inPeriod.stream().filter(i -> i.getStatus() == InvoiceStatus.OVERDUE).count();

        String currency = inPeriod.stream().map(Invoice::getCurrency).findFirst()
                .orElseGet(() -> all.stream().map(Invoice::getCurrency).findFirst().orElse("EUR"));

        return new StatementView(
                accountId,
                start,
                end,
                currency,
                MoneyUtil.roundHalfUp(opening),
                MoneyUtil.roundHalfUp(invoiced),
                MoneyUtil.roundHalfUp(paid),
                MoneyUtil.roundHalfUp(lateFees),
                MoneyUtil.roundHalfUp(opening + invoiced - paid),
                overdue,
                inPeriod.stream().map(InvoiceView::of).collect(Collectors.toList()));
    }
}
