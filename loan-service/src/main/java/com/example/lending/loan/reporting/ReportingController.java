package com.example.lending.loan.reporting;

import com.example.lending.loan.dto.ApiResult;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ReportingController {

    private final ReportStreamService streamService;
    private final CollectionsCalendar calendar;
    private final BenchmarkRateService benchmarkRateService;
    private final ReportScheduleService scheduleService;
    private final CollectionBucketService collectionBucketService;

    public ReportingController(ReportStreamService streamService, CollectionsCalendar calendar,
                               BenchmarkRateService benchmarkRateService, ReportScheduleService scheduleService,
                               CollectionBucketService collectionBucketService) {
        this.streamService = streamService;
        this.calendar = calendar;
        this.benchmarkRateService = benchmarkRateService;
        this.scheduleService = scheduleService;
        this.collectionBucketService = collectionBucketService;
    }

    @GetMapping(value = "/reports/collections/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String region) {
        return streamService.streamCollections(region);
    }

    @GetMapping("/reports/collections/next-business-day")
    public LocalDate nextBusinessDay(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from)
            throws IOException, InterruptedException {
        return calendar.nextBusinessDay(from);
    }

    @GetMapping("/reports/benchmark-rates")
    public List<RateFeedFetcher.RateFeed> benchmarkRates() {
        return benchmarkRateService.latest();
    }

    @PostMapping("/admin/reporting/schedules")
    public ApiResult createSchedule(@RequestBody ReportScheduleService.ScheduleRequest request) {
        return ApiResult.success(scheduleService.createSchedule(request));
    }

    @PostMapping("/admin/reporting/buckets/update/{id}")
    public ApiResult update(@PathVariable Long id, @RequestParam String name) {
        int count = collectionBucketService.update(id, name);
        if (count > 0) {
            return ApiResult.success(count);
        } else {
            return ApiResult.error("Bucket not found");
        }
    }

    @DeleteMapping("/admin/reporting/buckets")
    public ApiResult removeById(@RequestBody Long[] ids) {
        collectionBucketService.deleteBucketsAndRules(ids);
        return ApiResult.success(null);
    }

    @ExceptionHandler({IllegalArgumentException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ApiResult> onDuplicateSchedule(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResult.error("report schedule path already exists"));
    }
}
