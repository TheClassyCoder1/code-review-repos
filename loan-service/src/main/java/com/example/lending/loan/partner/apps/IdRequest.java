package com.example.lending.loan.partner.apps;

import jakarta.validation.constraints.NotNull;

public class IdRequest {

    @NotNull
    private Long id;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
