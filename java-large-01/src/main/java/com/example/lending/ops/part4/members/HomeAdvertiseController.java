package com.example.lending.ops.part4.members;

import com.example.lending.ops.common.Result.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/console/home/advertise")
@RequiredArgsConstructor
public class HomeAdvertiseController {

    private final SmsHomeAdvertiseService advertiseService;

    @PostMapping("/create")
    public CommonResult<Integer> create(@RequestBody SmsHomeAdvertise advertise) {
        int count = advertiseService.create(advertise);
        return count > 0 ? CommonResult.success(count) : CommonResult.failed();
    }
}
