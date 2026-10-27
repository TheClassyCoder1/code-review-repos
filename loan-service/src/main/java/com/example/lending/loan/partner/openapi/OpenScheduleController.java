package com.example.lending.loan.partner.openapi;

import com.example.lending.loan.support.web.ApiResponse;
import com.google.common.base.Preconditions;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/openapi")
public class OpenScheduleController {

    private final RepaymentScheduleService scheduleService;

    public OpenScheduleController(RepaymentScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @RequestMapping(value = "/schedule/list", method = RequestMethod.GET)
    public ApiResponse<List<RepaymentSchedule>> list(
            @RequestAttribute(name = OpenApiCaller.ATTRIBUTE, required = false) OpenApiCaller caller,
            @RequestParam String portfolioId) {
        Preconditions.checkArgument(caller != null, "login user cannot be empty");
        return ApiResponse.ofSuccess(scheduleService.list(caller.tenantId(), portfolioId));
    }

    @RequestMapping(value = "/schedule/get", method = RequestMethod.GET)
    public ApiResponse<RepaymentSchedule> get(
            @RequestAttribute(name = OpenApiCaller.ATTRIBUTE, required = false) OpenApiCaller caller,
            @RequestParam String portfolioId, @RequestParam String scheduleId) {
        Preconditions.checkArgument(!portfolioId.isBlank(), "portfolioId cannot be blank");
        Preconditions.checkArgument(!scheduleId.isBlank(), "scheduleId cannot be blank");
        Preconditions.checkArgument(caller != null, "login user cannot be empty");
        return ApiResponse.ofSuccess(scheduleService.get(portfolioId, scheduleId));
    }
}
