package com.example.lending.loan.reporting;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ReportScheduleService {

    public record ScheduleRequest(String path, String cron, String description) {
    }

    public record ScheduleChangedEvent(String operation, List<String> scheduleIds) {
    }

    private final ReportScheduleRepository scheduleRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<String, String> activeCrons = new ConcurrentHashMap<>();

    public ReportScheduleService(ReportScheduleRepository scheduleRepository, ApplicationEventPublisher eventPublisher) {
        this.scheduleRepository = scheduleRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public String createSchedule(ScheduleRequest request) {
        if (request.path() == null || !CronExpression.isValidExpression(request.cron())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "path and a valid cron are required");
        }
        return create(request);
    }

    private String create(final ScheduleRequest request) {
        Assert.isNull(scheduleRepository.findByPath(request.path()), "report schedule path already exists");
        ReportSchedule schedule = new ReportSchedule();
        schedule.setId(UUID.randomUUID().toString());
        schedule.setPath(request.path());
        schedule.setCron(request.cron());
        schedule.setDescription(Objects.isNull(request.description()) ? "" : request.description());
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        schedule.setDateCreated(currentTime);
        schedule.setDateUpdated(currentTime);
        if (scheduleRepository.saveAndFlush(schedule) != null) {
            activeCrons.put(schedule.getId(), schedule.getCron());
        }

        eventPublisher.publishEvent(new ScheduleChangedEvent("CREATE", Collections.singletonList(schedule.getId())));
        return schedule.getId();
    }
}
