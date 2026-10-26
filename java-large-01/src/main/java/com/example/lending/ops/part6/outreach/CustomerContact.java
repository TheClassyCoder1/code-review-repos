package com.example.lending.ops.part6.outreach;

import lombok.Data;

import java.time.LocalDate;

/** Contact details and channel preferences for a borrower, as held by the ops console. */
@Data
public class CustomerContact {
    private Long customerId;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private boolean emailOptIn;
    private boolean smsOptIn;
    private String preferredLanguage;
}
