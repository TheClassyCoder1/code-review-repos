package com.example.lending.ops.part2.admin;

import com.example.lending.ops.common.Result.CommonResult;
import com.example.lending.ops.part2.admin.AdminConsoleService.OmsMoneyInfoParam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/ops/admin/order")
@RequiredArgsConstructor
public class LoanOrderController {
    private final AdminConsoleService orderService;

    @Operation(summary = "修改订单费用信息")
    @RequestMapping(value = "/update/moneyInfo", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult updateReceiverInfo(@RequestBody OmsMoneyInfoParam moneyInfoParam) {
        int count = orderService.updateMoneyInfo(moneyInfoParam);
        if (count > 0) {
            return CommonResult.success(count);
        }
        return CommonResult.failed();
    }
}
