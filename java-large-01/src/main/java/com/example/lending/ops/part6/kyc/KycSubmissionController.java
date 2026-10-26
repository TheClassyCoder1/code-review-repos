package com.example.lending.ops.part6.kyc;

import com.example.lending.ops.common.Result;
import com.example.lending.ops.security.LoginUserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Agent-assisted KYC capture and review queue. */
@RestController
@RequestMapping("/console/kyc")
@RequiredArgsConstructor
public class KycSubmissionController {

    private final KycSubmissionService kycSubmissionService;

    @PostMapping("/customers/{customerId}/identity")
    public Result<Long> capture(@PathVariable Long customerId, @RequestBody KycIdentityRecord record) {
        try {
            return Result.OK(kycSubmissionService.submit(customerId, record));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/review-queue")
    public Result<List<KycIdentityRecord>> queue(@RequestParam(defaultValue = "50") int limit) {
        List<KycIdentityRecord> queue = kycSubmissionService.reviewQueue(limit);
        queue.forEach(r -> r.setNationalId(null));
        return Result.OK(queue);
    }

    @PostMapping("/identity/{recordId}/review")
    public Result<Void> review(@PathVariable Long recordId, @RequestParam boolean approved) {
        try {
            LoginUserUtils.ApiCaller caller = LoginUserUtils.getLoginUser();
            if (caller == null) {
                return Result.error("not authenticated");
            }
            kycSubmissionService.review(recordId, caller.userId(), approved);
            return Result.OK(null);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
}
