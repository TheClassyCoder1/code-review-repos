package com.example.lending.ops.part5.maintenance;

import com.example.lending.ops.part5.apidoc.Operation;
import com.example.lending.ops.part5.maintenance.web.ApiAccessLog;
import com.example.lending.ops.part5.maintenance.web.ExcelUtils;
import com.example.lending.ops.part5.maintenance.web.PageParam;
import com.example.lending.ops.part5.maintenance.web.Valid;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

import static com.example.lending.ops.part5.maintenance.web.ApiAccessLog.OperateType.EXPORT;

/** Branch equipment maintenance record lines. */
@RestController
@RequestMapping("/mes/dv/mainten-record-line")
@RequiredArgsConstructor
public class MesDvMaintenRecordLineController {

    private final MesDvMaintenRecordLineService maintenRecordLineService;

    @GetMapping("/export-excel")
    @Operation(summary = "导出设备保养记录明细 Excel")
    @PreAuthorize("@ss.hasPermission('mes:dv-mainten-record:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportMaintenRecordLineExcel(@Valid MesDvMaintenRecordLinePageReqVO pageReqVO,
                                              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<MesDvMaintenRecordLineDO> list = maintenRecordLineService.getMaintenRecordLinePage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "设备保养记录明细.xls", "数据", MesDvMaintenRecordLineRespVO.class,
                buildMaintenRecordLineRespVOList(list));
    }

    private List<MesDvMaintenRecordLineRespVO> buildMaintenRecordLineRespVOList(List<MesDvMaintenRecordLineDO> list) {
        return list.stream()
                .map(d -> new MesDvMaintenRecordLineRespVO(d.getId(), d.getRecordId(), d.getSubjectCode(),
                        d.getResult(), d.getRemark(), d.getTechnicianUserId(), d.getCheckedAt()))
                .toList();
    }
}
