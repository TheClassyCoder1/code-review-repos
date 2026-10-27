package com.example.lending.ops.part5.maintenance;

import lombok.Data;

import java.time.LocalDateTime;

/** One checklist line of a branch kiosk / cash-recycler maintenance visit. */
@Data
public class MesDvMaintenRecordLineDO {
    private Long id;
    private Long recordId;
    private String subjectCode;
    private String result;
    private String remark;
    private Long technicianUserId;
    private LocalDateTime checkedAt;
}
