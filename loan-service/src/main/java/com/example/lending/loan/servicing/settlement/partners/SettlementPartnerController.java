package com.example.lending.loan.servicing.settlement.partners;

import com.example.lending.loan.servicing.common.CurrentOperator;
import com.example.lending.loan.servicing.common.ServicingResult;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Settlement partner administration for the operator's tenant. */
@RestController
@RequestMapping("/servicing/settlement/partners")
public class SettlementPartnerController {

    private final SettlementPartnerService partnerService;
    private final CurrentOperator currentOperator;

    public SettlementPartnerController(SettlementPartnerService partnerService, CurrentOperator currentOperator) {
        this.partnerService = partnerService;
        this.currentOperator = currentOperator;
    }

    @GetMapping(value = {"/list"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('partner:read')")
    public ServicingResult<List<PartnerSummary>> list() {
        return ServicingResult.ok(partnerService.listForTenant(currentOperator.tenantId()).stream()
                .map(PartnerSummary::of).toList());
    }

    @GetMapping(value = {"/get/{id}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @PreAuthorize("hasAuthority('partner:read')")
    public ServicingResult<SettlementPartner> get(@PathVariable String id) {
        SettlementPartner partner = partnerService.get(id);
        partnerService.decoderSecret(partner);
        partner.transLogoBase64();
        return ServicingResult.ok(partner);
    }


    public record PartnerSummary(String id, String name, String clientId) {

        static PartnerSummary of(SettlementPartner partner) {
            return new PartnerSummary(partner.getId(), partner.getName(), partner.getClientId());
        }
    }

}
