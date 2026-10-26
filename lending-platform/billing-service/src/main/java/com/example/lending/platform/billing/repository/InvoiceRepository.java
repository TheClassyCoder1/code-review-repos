package com.example.lending.platform.billing.repository;

import com.example.lending.platform.billing.entity.Invoice;
import com.example.lending.platform.billing.entity.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    List<Invoice> findByAccountIdOrderByIssueDateDesc(Long accountId);

    List<Invoice> findByAccountIdAndIssueDateBetweenOrderByIssueDateAsc(Long accountId, LocalDate from, LocalDate to);

    List<Invoice> findByStatusInAndDueDateBefore(Collection<InvoiceStatus> statuses, LocalDate date);

    long countByAccountIdAndStatus(Long accountId, InvoiceStatus status);

    @Query("select coalesce(max(i.id), 0) from Invoice i")
    long maxId();

    @Query("select coalesce(sum(i.totalAmount - i.paidAmount), 0) from Invoice i "
            + "where i.accountId = :accountId and i.status in :statuses")
    double sumOutstanding(@Param("accountId") Long accountId, @Param("statuses") Collection<InvoiceStatus> statuses);
}
