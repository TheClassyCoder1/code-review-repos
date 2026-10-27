package com.example.lending.loan.part7.controller;

import com.example.lending.loan.part7.dto.RepaymentScheduleDto;
import com.example.lending.loan.part7.service.RepaymentScheduleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.NoSuchElementException;

/** New read-only endpoint; does not alter the existing /api/v1/loans/{id} shape. */
@RestController
@RequestMapping("/api/v1/loans")
public class RepaymentScheduleController {

    private final RepaymentScheduleService scheduleService;

    public RepaymentScheduleController(RepaymentScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/{id}/schedule")
    public RepaymentScheduleDto schedule(
            @PathVariable Long id,
            @RequestParam(required = false) Integer termMonths,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate firstDueDate) {
        try {
            return scheduleService.scheduleFor(id, termMonths, firstDueDate);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
