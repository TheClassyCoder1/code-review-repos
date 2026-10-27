package com.example.lending.loan.config;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Jackson mix-in for {@link com.example.lending.loan.entity.Loan}: monetary amounts are written as
 * decimal strings so JavaScript clients never round them through IEEE-754 doubles.
 */
abstract class LoanAmountJsonMixin {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    abstract double getAmount();
}
