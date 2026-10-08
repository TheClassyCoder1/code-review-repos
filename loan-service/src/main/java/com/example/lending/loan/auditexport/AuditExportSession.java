package com.example.lending.loan.auditexport;

import com.example.lending.loan.entity.AuditLog;
import com.example.lending.loan.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** One running audit export. Counts against the concurrent export limit until closed. */
public class AuditExportSession implements AutoCloseable {

    private final AuditLogRepository auditLogRepository;
    private final AtomicInteger concurrentExports;
    private final AtomicBoolean closed = new AtomicBoolean();

    AuditExportSession(AuditLogRepository auditLogRepository, AtomicInteger concurrentExports) {
        this.auditLogRepository = auditLogRepository;
        this.concurrentExports = concurrentExports;
        concurrentExports.incrementAndGet();
    }

    public void writeCsv(OutputStream out) throws IOException {
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        writer.write("id,entity_type,message\n");
        Page<AuditLog> page;
        int number = 0;
        do {
            page = auditLogRepository.findAll(PageRequest.of(number++, 500, Sort.by("id")));
            for (AuditLog log : page) {
                writer.write(log.getId() + "," + csv(log.getEntityType()) + "," + csv(log.getMessage()) + "\n");
            }
            writer.flush();
        } while (page.hasNext());
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            concurrentExports.decrementAndGet();
        }
    }

    private static String csv(String value) {
        return value == null ? "" : "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
