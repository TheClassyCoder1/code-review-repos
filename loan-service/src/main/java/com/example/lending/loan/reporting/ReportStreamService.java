package com.example.lending.loan.reporting;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

@Service
public class ReportStreamService {

    private static final String COLLECTIONS_SQL = "select loan_id, region, due_date, status, days_past_due "
            + "from lending.collection_cases where region = ? order by due_date, loan_id limit ? offset ?";
    private static final int PAGE_SIZE = 500;

    private final JdbcTemplate jdbcTemplate;
    private final ReportStreamWriter writer;
    private final TaskExecutor taskExecutor;

    public ReportStreamService(JdbcTemplate jdbcTemplate, ReportStreamWriter writer,
                               @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.jdbcTemplate = jdbcTemplate;
        this.writer = writer;
        this.taskExecutor = taskExecutor;
    }

    public SseEmitter streamCollections(String region) {
        SseEmitter emitter = new SseEmitter(120_000L);
        taskExecutor.execute(() -> {
            try {
                List<Map<String, Object>> page;
                int offset = 0;
                do {
                    page = jdbcTemplate.queryForList(COLLECTIONS_SQL, region, PAGE_SIZE, offset);
                    writer.sendChunk(emitter, page, null);
                    offset += PAGE_SIZE;
                } while (page.size() == PAGE_SIZE);
                writer.sendChunk(emitter, null, "done");
                emitter.complete();
            } catch (Exception e) {
                writer.handleError(emitter, new IllegalStateException("Report generation failed", e));
            }
        });
        return emitter;
    }
}
