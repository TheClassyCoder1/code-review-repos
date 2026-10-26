package com.example.lending.platform.billing.service;

import com.example.lending.platform.billing.client.AccountClient;
import com.example.lending.platform.billing.dto.CreateInvoiceRequest;
import com.example.lending.platform.billing.dto.PaymentRequest;
import com.example.lending.platform.billing.entity.Invoice;
import com.example.lending.platform.billing.entity.InvoiceLine;
import com.example.lending.platform.billing.entity.InvoiceStatus;
import com.example.lending.platform.billing.invoice.InvoiceException;
import com.example.lending.platform.billing.invoice.InvoiceNumberGenerator;
import com.example.lending.platform.billing.kafka.InvoiceEventProducer;
import com.example.lending.platform.billing.repository.InvoiceRepository;
import com.example.lending.platform.common.dto.AccountDto;
import com.example.lending.platform.common.util.MoneyUtil;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Invoice lifecycle: drafting, issuing, payments, disputes and voiding.
 * Every state change goes through {@link #transition} so the allowed moves live in one place
 * ({@link InvoiceStatus#canTransitionTo}).
 */
@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("EUR", "GBP", "USD", "INR");
    private static final int MAX_LINES = 50;

    private final InvoiceRepository invoiceRepository;
    private final InvoiceNumberGenerator numberGenerator;
    private final InvoiceEventProducer eventProducer;
    private final AccountClient accountClient;
    private final Clock clock;

    public InvoiceService(InvoiceRepository invoiceRepository, InvoiceNumberGenerator numberGenerator,
                          InvoiceEventProducer eventProducer, AccountClient accountClient, Clock billingClock) {
        this.invoiceRepository = invoiceRepository;
        this.numberGenerator = numberGenerator;
        this.eventProducer = eventProducer;
        this.accountClient = accountClient;
        this.clock = billingClock;
    }

    @Transactional
    public Invoice create(CreateInvoiceRequest request) {
        if (request.getAccountId() == null) {
            throw InvoiceException.invalid("accountId is required");
        }
        String currency = request.getCurrency() == null ? "" : request.getCurrency().trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_CURRENCIES.contains(currency)) {
            throw InvoiceException.invalid("unsupported currency: " + request.getCurrency());
        }
        if (request.getLines() == null || request.getLines().isEmpty()) {
            throw InvoiceException.invalid("an invoice needs at least one line");
        }
        if (request.getLines().size() > MAX_LINES) {
            throw InvoiceException.invalid("an invoice may have at most " + MAX_LINES + " lines");
        }
        if (request.getDueDate() == null) {
            throw InvoiceException.invalid("dueDate is required");
        }
        requireActiveAccount(request.getAccountId());

        LocalDate issueDate = request.getIssueDate() != null ? request.getIssueDate() : LocalDate.now(clock);

        Invoice invoice = new Invoice();
        invoice.setAccountId(request.getAccountId());
        invoice.setLoanId(request.getLoanId());
        invoice.setCurrency(currency);
        invoice.setIssueDate(issueDate);
        invoice.setDueDate(request.getDueDate());
        invoice.setInvoiceNumber(numberGenerator.next(issueDate));
        for (CreateInvoiceRequest.Line line : request.getLines()) {
            String kind = line.getKind() == null ? "ADJUSTMENT" : line.getKind().toUpperCase(Locale.ROOT);
            invoice.addLine(new InvoiceLine(kind, line.getDescription(), line.getQuantity(), line.getUnitAmount()));
        }
        invoice.setTotalAmount(MoneyUtil.roundHalfUp(invoice.getLines().stream().mapToDouble(InvoiceLine::lineTotal).sum()));

        Invoice saved = invoiceRepository.save(invoice);
        log.info("created invoice {} for account {}", saved.getId(), saved.getAccountId());
        if (request.isIssueImmediately()) {
            return issue(saved.getId());
        }
        return saved;
    }

    @Transactional
    public Invoice issue(Long invoiceId) {
        Invoice invoice = get(invoiceId);
        transition(invoice, InvoiceStatus.ISSUED);
        if (invoice.getIssueDate() == null) {
            invoice.setIssueDate(LocalDate.now(clock));
        }
        Invoice saved = invoiceRepository.save(invoice);
        eventProducer.invoiceIssued(saved);
        return saved;
    }

    @Transactional
    public Invoice recordPayment(Long invoiceId, PaymentRequest payment) {
        if (payment.getAmount() <= 0) {
            throw InvoiceException.invalid("payment amount must be positive");
        }
        Invoice invoice = get(invoiceId);
        if (!invoice.getStatus().isOpen()) {
            throw InvoiceException.invalidState(invoiceId, invoice.getStatus(), "payment");
        }
        double paid = MoneyUtil.roundHalfUp(invoice.getPaidAmount() + payment.getAmount());
        if (paid > invoice.getTotalAmount()) {
            throw InvoiceException.invalid("payment exceeds outstanding amount of " + invoice.outstandingAmount());
        }
        invoice.setPaidAmount(paid);
        log.info("recorded payment {} on invoice {}", payment.getReference(), invoiceId);

        if (invoice.outstandingAmount() == 0) {
            if (invoice.getStatus() == InvoiceStatus.DISPUTED) {
                transition(invoice, InvoiceStatus.ISSUED);
            }
            transition(invoice, InvoiceStatus.PAID);
            Invoice saved = invoiceRepository.save(invoice);
            eventProducer.invoicePaid(saved);
            return saved;
        }
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice dispute(Long invoiceId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw InvoiceException.invalid("a dispute reason is required");
        }
        Invoice invoice = get(invoiceId);
        transition(invoice, InvoiceStatus.DISPUTED);
        log.info("invoice {} disputed", invoiceId);
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice resolveDispute(Long invoiceId, boolean upheld) {
        Invoice invoice = get(invoiceId);
        if (invoice.getStatus() != InvoiceStatus.DISPUTED) {
            throw InvoiceException.invalidState(invoiceId, invoice.getStatus(), upheld ? "VOID" : "ISSUED");
        }
        transition(invoice, upheld ? InvoiceStatus.VOID : InvoiceStatus.ISSUED);
        return invoiceRepository.save(invoice);
    }

    @Transactional
    public Invoice voidInvoice(Long invoiceId) {
        Invoice invoice = get(invoiceId);
        if (invoice.getPaidAmount() > 0) {
            throw InvoiceException.invalid("invoice " + invoiceId + " has payments and must be refunded first");
        }
        transition(invoice, InvoiceStatus.VOID);
        return invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public Invoice get(Long invoiceId) {
        return invoiceRepository.findById(invoiceId).orElseThrow(() -> InvoiceException.notFound(invoiceId));
    }

    @Transactional(readOnly = true)
    public List<Invoice> listForAccount(Long accountId) {
        return invoiceRepository.findByAccountIdOrderByIssueDateDesc(accountId);
    }

    void transition(Invoice invoice, InvoiceStatus target) {
        InvoiceStatus current = invoice.getStatus();
        if (!current.canTransitionTo(target)) {
            throw InvoiceException.invalidState(invoice.getId(), current, target);
        }
        invoice.setStatus(target);
    }

    private void requireActiveAccount(Long accountId) {
        AccountDto account;
        try {
            account = accountClient.getAccount(accountId);
        } catch (FeignException e) {
            throw new InvoiceException(InvoiceException.Code.ACCOUNT_UNAVAILABLE,
                    "account-service lookup failed for account " + accountId);
        }
        // account-service answers 200 with an empty body for unknown ids
        if (account == null) {
            throw InvoiceException.invalid("account not found: " + accountId);
        }
        if ("CLOSED".equalsIgnoreCase(account.getStatus())) {
            throw InvoiceException.invalid("account " + accountId + " is closed");
        }
    }
}
