package com.example.lending.ops.part6.outreach;

import com.example.lending.ops.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Collections outreach operations and contact-preference maintenance for agents. */
@RestController
@RequestMapping("/console/outreach")
@RequiredArgsConstructor
public class OutreachController {

    private final CollectionsOutreachService outreachService;
    private final ContactPreferenceService contactPreferenceService;

    @PostMapping("/run")
    public Result<CollectionsOutreachService.OutreachSummary> run() {
        return Result.OK(outreachService.runDailyOutreach());
    }

    @GetMapping("/contacts/{customerId}")
    public Result<CustomerContact> contact(@PathVariable Long customerId) {
        return contactPreferenceService.find(customerId)
                .map(Result::OK)
                .orElseGet(() -> Result.error("no contact record"));
    }

    @PutMapping("/contacts/{customerId}")
    public Result<Void> update(@PathVariable Long customerId, @RequestBody CustomerContact contact) {
        try {
            contactPreferenceService.update(customerId, contact);
            return Result.OK(null);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/contacts/{customerId}/sms-opt-out")
    public Result<Void> smsOptOut(@PathVariable Long customerId) {
        contactPreferenceService.optOutOfSms(customerId);
        return Result.OK(null);
    }
}
