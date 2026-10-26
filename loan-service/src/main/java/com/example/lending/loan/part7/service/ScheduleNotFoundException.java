package com.example.lending.loan.part7.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ScheduleNotFoundException extends RuntimeException {

    public ScheduleNotFoundException(Long loanId) {
        super("no repayment schedule for loan " + loanId);
    }
}
