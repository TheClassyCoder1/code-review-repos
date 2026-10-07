package com.example.lending.loan.controller;

import com.example.lending.loan.dto.ApiResult;
import com.example.lending.loan.entity.CollectionSchedule;
import com.example.lending.loan.service.CollectionScheduleService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/collection-schedules")
public class CollectionScheduleController {

    private final CollectionScheduleService collectionScheduleService;

    public CollectionScheduleController(CollectionScheduleService collectionScheduleService) {
        this.collectionScheduleService = collectionScheduleService;
    }

    @PostMapping("/{id}/start")
    public ApiResult startSchedule(@PathVariable("id") Long scheduleId) {
        CollectionSchedule schedule = collectionScheduleService.getById(scheduleId);
        if (schedule != null && CollectionSchedule.STATUS_FAILED.equals(schedule.getStatus())) {
            collectionScheduleService.reschedule(schedule);
        }
        else {
            collectionScheduleService.resume(schedule);
        }
        collectionScheduleService.updateStatus(scheduleId, CollectionSchedule.STATUS_RUNNING);
        return ApiResult.success(null);
    }

    @GetMapping("/cron")
    public ApiResult cronByName(@RequestParam String name) {
        return ApiResult.success(collectionScheduleService.findCronByName(name));
    }
}
