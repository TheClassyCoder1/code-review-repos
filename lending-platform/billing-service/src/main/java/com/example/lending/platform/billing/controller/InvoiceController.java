package com.example.lending.platform.billing.controller;

import com.example.lending.platform.billing.dto.CreateInvoiceRequest;
import com.example.lending.platform.billing.dto.InvoiceView;
import com.example.lending.platform.billing.dto.PaymentRequest;
import com.example.lending.platform.billing.dto.StatementView;
import com.example.lending.platform.billing.service.DunningService;
import com.example.lending.platform.billing.service.InvoiceService;
import com.example.lending.platform.billing.service.StatementService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/billing")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final StatementService statementService;
    private final DunningService dunningService;

    public InvoiceController(InvoiceService invoiceService, StatementService statementService,
                             DunningService dunningService) {
        this.invoiceService = invoiceService;
        this.statementService = statementService;
        this.dunningService = dunningService;
    }

    @PostMapping("/invoices")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceView create(@RequestBody CreateInvoiceRequest request) {
        return InvoiceView.of(invoiceService.create(request));
    }

    @GetMapping("/invoices/{invoiceId}")
    public InvoiceView get(@PathVariable Long invoiceId) {
        return InvoiceView.of(invoiceService.get(invoiceId));
    }

    @GetMapping("/accounts/{accountId}/invoices")
    public List<InvoiceView> listForAccount(@PathVariable Long accountId) {
        return invoiceService.listForAccount(accountId).stream().map(InvoiceView::of).collect(Collectors.toList());
    }

    @PostMapping("/invoices/{invoiceId}/issue")
    public InvoiceView issue(@PathVariable Long invoiceId) {
        return InvoiceView.of(invoiceService.issue(invoiceId));
    }

    @PostMapping("/invoices/{invoiceId}/payments")
    public InvoiceView recordPayment(@PathVariable Long invoiceId, @RequestBody PaymentRequest payment) {
        return InvoiceView.of(invoiceService.recordPayment(invoiceId, payment));
    }

    @PostMapping("/invoices/{invoiceId}/dispute")
    public InvoiceView dispute(@PathVariable Long invoiceId, @RequestBody Map<String, String> body) {
        return InvoiceView.of(invoiceService.dispute(invoiceId, body.get("reason")));
    }

    @PostMapping("/invoices/{invoiceId}/dispute/resolve")
    public InvoiceView resolveDispute(@PathVariable Long invoiceId, @RequestParam boolean upheld) {
        return InvoiceView.of(invoiceService.resolveDispute(invoiceId, upheld));
    }

    @PostMapping("/invoices/{invoiceId}/void")
    public InvoiceView voidInvoice(@PathVariable Long invoiceId) {
        return InvoiceView.of(invoiceService.voidInvoice(invoiceId));
    }

    @GetMapping("/accounts/{accountId}/statements/{month}")
    public StatementView statement(@PathVariable Long accountId, @PathVariable String month) {
        return statementService.monthly(accountId, YearMonth.parse(month));
    }

    @PostMapping("/admin/dunning/run")
    public DunningService.DunningRun runDunning() {
        return dunningService.run();
    }
}
