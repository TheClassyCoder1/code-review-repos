package com.example.lending.platform.billing.invoice;

import com.example.lending.platform.billing.repository.InvoiceRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Produces human-readable invoice numbers of the form {@code INV-202610-000123}.
 * The sequence is seeded from the highest stored id on first use so restarts do not reuse numbers;
 * the unique constraint on {@code invoice_number} is the final safeguard across replicas.
 */
@Component
public class InvoiceNumberGenerator {

    private static final DateTimeFormatter PERIOD = DateTimeFormatter.ofPattern("yyyyMM");

    private final InvoiceRepository invoiceRepository;
    private final AtomicLong sequence = new AtomicLong(-1);

    public InvoiceNumberGenerator(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public String next(LocalDate issueDate) {
        if (sequence.get() < 0) {
            sequence.compareAndSet(-1, invoiceRepository.maxId());
        }
        long value = sequence.incrementAndGet();
        return String.format("INV-%s-%06d", issueDate.format(PERIOD), value);
    }
}
