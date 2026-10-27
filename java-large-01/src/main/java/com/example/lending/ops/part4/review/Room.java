package com.example.lending.ops.part4.review;

import java.io.Serializable;

/** A case-review room; one per loan application under committee review. */
public record Room(Long id, String loanApplicationId) implements Serializable {
}
