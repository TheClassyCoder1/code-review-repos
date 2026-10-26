package com.example.lending.ops.part5.maintenance;

import java.time.LocalDateTime;

/** Export row; technician is identified by internal user id only. */
public record MesDvMaintenRecordLineRespVO(Long id, Long recordId, String subjectCode, String result,
        String remark, Long technicianUserId, LocalDateTime checkedAt) {
}
