package com.example.lending.ops.console;

import com.example.lending.ops.common.Result.CommonResult;
import com.example.lending.ops.common.apidoc.ApiOperation.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** Disbursement order maintenance; the whole path is restricted to OPS_ADMIN by ConsoleSecurityConfig. */
@Controller
@RequestMapping("/console/orders")
@RequiredArgsConstructor
public class DisbursementOrderController {

    private final ConsoleUserService orderService;

    @Operation(summary = "批量删除订单")
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult delete(@RequestParam("ids") List<Long> ids) {
        int count = orderService.delete(ids);
        if (count > 0) {
            return CommonResult.success(count);
        }
        return CommonResult.failed();
    }
}
