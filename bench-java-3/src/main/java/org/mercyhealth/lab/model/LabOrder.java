package org.mercyhealth.lab.model;

import java.time.Instant;

public record LabOrder(long id, String patientMrn, long physicianId, String testCode,
                       String priority, String status, Instant orderedAt) {
}
