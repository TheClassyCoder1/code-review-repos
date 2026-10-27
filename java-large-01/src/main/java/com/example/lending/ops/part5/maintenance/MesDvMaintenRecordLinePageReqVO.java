package com.example.lending.ops.part5.maintenance;

import com.example.lending.ops.part5.maintenance.web.PageParam;
import lombok.Getter;
import lombok.Setter;

/** Filters for the maintenance record line list and export. */
@Getter
@Setter
public class MesDvMaintenRecordLinePageReqVO extends PageParam {
    private Long recordId;
    private String result;
}
