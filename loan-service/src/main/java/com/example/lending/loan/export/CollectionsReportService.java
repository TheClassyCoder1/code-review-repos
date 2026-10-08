package com.example.lending.loan.export;

import com.example.lending.loan.document.DocumentStorage;
import com.example.lending.loan.reporting.LoggingReportTracer;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.sql.Date;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class CollectionsReportService {

    public record Request(String region, LocalDate from, LocalDate to, String status,
                          List<ReportLayout.Field> fieldOverrides) {
    }

    private static final String REPORT_TYPE = "collections";
    private static final String OVERDUE_SQL =
            "select l.id as loan_id, l.tier, l.amount, c.region, c.due_date, c.status "
                    + "from lending.loans l join lending.collection_cases c on c.loan_id = l.id "
                    + "where c.region = ? and c.due_date between ? and ? and c.status = ? order by c.due_date";

    private final JdbcTemplate jdbcTemplate;
    private final ReportExportRepository exportRepository;
    private final DocumentStorage storage;
    private final LoggingReportTracer tracer;
    private final Map<ReportKey<String>, ReportLayout> layouts = new ConcurrentHashMap<>();

    public CollectionsReportService(JdbcTemplate jdbcTemplate, ReportExportRepository exportRepository,
                                    DocumentStorage storage, LoggingReportTracer tracer) {
        this.jdbcTemplate = jdbcTemplate;
        this.exportRepository = exportRepository;
        this.storage = storage;
        this.tracer = tracer;
    }

    public ReportExportJob run(Long ownerId, Request request) throws IOException {
        if (request.region() == null || request.from() == null || request.to() == null
                || request.to().isBefore(request.from())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "region and a valid date range are required");
        }
        ReportLayout layout = layouts.computeIfAbsent(new ReportKey<>(REPORT_TYPE), k -> ReportLayout.collections())
                .withOverrides(request.fieldOverrides());
        String sql = ReportQueryBinder.bind(OVERDUE_SQL, List.of(
                ReportQueryBinder.Param.of(request.region()),
                ReportQueryBinder.Param.of(Date.valueOf(request.from())),
                ReportQueryBinder.Param.of(Date.valueOf(request.to())),
                ReportQueryBinder.Param.of(Objects.requireNonNullElse(request.status(), "OVERDUE"))));
        tracer.trace("collections report query", REPORT_TYPE);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        ReportExportJob job = new ReportExportJob();
        job.setOwnerId(ownerId);
        job.setReportType(REPORT_TYPE);
        job.setCreatedAt(Instant.now());
        job.setFileName(REPORT_TYPE + "-" + UUID.randomUUID() + ".csv");
        Path target = storage.getRoot().resolve("exports").resolve(job.getFileName());
        Files.createDirectories(target.getParent());
        List<String> lines = new ArrayList<>();
        lines.add(layout.getFields().stream().map(f -> f.label().replace(",", " ")).collect(Collectors.joining(",")));
        for (Map<String, Object> row : rows) {
            lines.add(layout.getFields().stream()
                    .map(f -> String.valueOf(row.getOrDefault(f.name(), "")).replace(",", " "))
                    .collect(Collectors.joining(",")));
        }
        Files.write(target, lines);
        job.setStatus("COMPLETED");
        return exportRepository.save(job);
    }
}
