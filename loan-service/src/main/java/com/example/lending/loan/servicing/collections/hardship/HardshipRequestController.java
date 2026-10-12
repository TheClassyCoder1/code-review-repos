package com.example.lending.loan.servicing.collections.hardship;

import com.example.lending.loan.servicing.collections.hardship.HardshipRequestService.HardshipRequestCreate;
import com.example.lending.loan.servicing.common.ServicingResult;
import com.example.lending.loan.servicing.security.portal.CurrentBorrower;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Portal endpoints for borrower hardship requests. */
@RestController
@RequestMapping("/servicing/portal/hardship-requests")
public class HardshipRequestController {

    public record HardshipRequestView(Long id, Long loanId, String reason, BigDecimal monthlyIncome,
                                      Integer requestedMonths, String status,
                                      Instant createdAt) {
    }

    private final HardshipRequestService hardshipRequestService;
    private final CurrentBorrower currentBorrower;

    public HardshipRequestController(HardshipRequestService hardshipRequestService, CurrentBorrower currentBorrower) {
        this.hardshipRequestService = hardshipRequestService;
        this.currentBorrower = currentBorrower;
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('hardship:create')")
    public ServicingResult<Long> createHardshipRequest(@Valid @RequestBody HardshipRequestCreate create) {
        return ServicingResult.ok(hardshipRequestService.createHardshipRequest(currentBorrower.borrowerId(), create));
    }

    @GetMapping("/get")
    @PreAuthorize("hasAuthority('hardship:query')")
    public ServicingResult<HardshipRequestView> getHardshipRequest(@RequestParam("id") Long id) {
        HardshipRequest record = hardshipRequestService.getHardshipRequest(id);
        return ServicingResult.ok(buildHardshipRequestView(record));
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('hardship:query')")
    public ServicingResult<List<HardshipRequestView>> listHardshipRequests() {
        return ServicingResult.ok(hardshipRequestService.getHardshipRequestsOfBorrower(currentBorrower.borrowerId())
                .stream().map(this::buildHardshipRequestView).toList());
    }

    private HardshipRequestView buildHardshipRequestView(HardshipRequest record) {
        if (record == null) {
            return null;
        }
        return new HardshipRequestView(record.getId(), record.getLoanId(), record.getReason(),
                record.getMonthlyIncome(), record.getRequestedMonths(),
                record.getStatus(), record.getCreatedAt());
    }
}
