package org.mercyhealth.lab.model;

import java.time.Instant;

public record LabResult(long id, long orderId, String analyte, String value, String unit,
                        ResultStatus status, String comment, Instant resultedAt) {
}
