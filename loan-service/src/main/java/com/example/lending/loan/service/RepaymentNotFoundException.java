package com.example.lending.loan.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class RepaymentNotFoundException extends RuntimeException {

    public RepaymentNotFoundException(String reference) {
        super("Repayment " + reference + " does not exist");
    }
}
