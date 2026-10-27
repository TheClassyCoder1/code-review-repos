package com.example.lending.ops.expense;

import com.example.lending.ops.common.Result.CommonResult;
import com.example.lending.ops.common.apidoc.ApiOperation.*;
import com.example.lending.ops.expense.OaTravelReimbursementService.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.example.lending.ops.common.Result.CommonResult.success;

@RestController
@RequestMapping("/oa/travel-reimbursement")
@RequiredArgsConstructor
public class OaTravelReimbursementController {

    private final OaTravelReimbursementService travelReimbursementService;

    @GetMapping("/get")
    @Operation(summary = "获得出差报销详情")
    @Parameter(name = "id", description = "报销编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('oa:travel-reimbursement:query')")
    public CommonResult<OaTravelReimbursementRespVO> getTravelReimbursement(@RequestParam("id") Long id) {
        OaTravelReimbursementDO record = travelReimbursementService.getTravelReimbursement(id);
        return success(buildTravelReimbursementRespVO(record));
    }

    private OaTravelReimbursementRespVO buildTravelReimbursementRespVO(OaTravelReimbursementDO r) {
        return r == null ? null : new OaTravelReimbursementRespVO(r.getId(), r.getApplicantUserId(),
                r.getLoanReference(), r.getTravelDate(), r.getAmount(), r.getStatus());
    }
}
