package com.example.lending.loan.service;

import com.example.lending.loan.entity.CollectionSchedule;
import com.example.lending.loan.repository.CollectionScheduleRepository;
import com.example.lending.loan.util.ScheduleConfigs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CollectionScheduleService {

    private static final ScheduleConfigs.Term DEFAULT_TERM = new ScheduleConfigs.Term(12, 30);

    private final CollectionScheduleRepository repository;
    private final Map<Long, ScheduleConfigs.Term> active = new ConcurrentHashMap<>();

    public CollectionScheduleService(CollectionScheduleRepository repository) {
        this.repository = repository;
    }

    public CollectionSchedule getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public void reschedule(CollectionSchedule schedule) {
        active.remove(schedule.getId());
        active.put(schedule.getId(), ScheduleConfigs.termOf(schedule.getConfig(), DEFAULT_TERM));
    }

    public void resume(CollectionSchedule schedule) {
        active.putIfAbsent(schedule.getId(), ScheduleConfigs.termOf(schedule.getConfig(), DEFAULT_TERM));
    }

    @Transactional
    public void updateStatus(Long id, String status) {
        repository.findById(id).ifPresent(schedule -> {
            schedule.setStatus(status);
            repository.save(schedule);
        });
    }

    public String findCronByName(String scheduleName) {
        if (!StringUtils.hasLength(scheduleName)) {
            return null;
        }

        CollectionSchedule schedule = repository.findByName(scheduleName);
        return schedule == null ? null : ScheduleConfigs.cronOf(schedule.getConfig());
    }
}
