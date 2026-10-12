package com.example.lending.loan.servicing.collections.waivers;

import com.example.lending.loan.servicing.common.ServicingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Portal endpoints for fee waiver campaigns. */
@RestController
@RequestMapping("/servicing/portal/fee-waivers")
public class FeeWaiverController {

    private final FeeWaiverClaimService claimService;

    public FeeWaiverController(FeeWaiverClaimService claimService) {
        this.claimService = claimService;
    }

    @PostMapping("/{campaignId}/claims")
    public ServicingResult<Void> claim(@PathVariable Long campaignId) {
        claimService.add(campaignId);
        return ServicingResult.ok();
    }

    @GetMapping("/claims")
    public ServicingResult<List<FeeWaiverClaim>> myClaims() {
        return ServicingResult.ok(claimService.listMine());
    }
}
