package com.example.lending.loan.servicing.recon.rules;

import com.example.lending.loan.servicing.common.ServicingResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/** Maintenance of matcher exclusion rules. Matcher instances reload rules when notified on the reload topic. */
@RestController
@Validated
@RequestMapping("/servicing/recon/exclusion-rules")
public class ReconExclusionRuleController {

    static final String RULES_RELOAD_TOPIC = "servicing.recon.rules.reload";

    private final ReconExclusionRuleService exclusionRuleService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public ReconExclusionRuleController(ReconExclusionRuleService exclusionRuleService,
                                        @Qualifier("brokerATemplate") KafkaTemplate<String, String> kafkaTemplate) {
        this.exclusionRuleService = exclusionRuleService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('recon_rule_view')")
    public ServicingResult<List<ReconExclusionRule>> list() {
        return ServicingResult.ok(exclusionRuleService.list());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('recon_rule_add')")
    public ServicingResult<Long> create(@RequestParam @NotBlank @Size(max = 140) String keyword) {
        Long id = exclusionRuleService.create(keyword).getId();
        kafkaTemplate.send(RULES_RELOAD_TOPIC, "reload");
        return ServicingResult.ok(id);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('recon_rule_del')")
    public ServicingResult<Void> removeById(@RequestBody Long[] ids) {
        exclusionRuleService.removeBatchByIds(Arrays.asList(ids));
        kafkaTemplate.send(RULES_RELOAD_TOPIC, "reload");
        return ServicingResult.ok();
    }
}
