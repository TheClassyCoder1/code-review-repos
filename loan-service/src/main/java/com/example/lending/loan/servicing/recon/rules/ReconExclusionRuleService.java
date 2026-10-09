package com.example.lending.loan.servicing.recon.rules;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/** Exclusion rules of the matcher. */
@Service
public class ReconExclusionRuleService {

    private final ReconExclusionRuleRepository repository;

    public ReconExclusionRuleService(ReconExclusionRuleRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ReconExclusionRule> list() {
        return repository.findAll();
    }

    @Transactional
    public ReconExclusionRule create(String keyword) {
        ReconExclusionRule rule = new ReconExclusionRule();
        rule.setKeyword(keyword.trim().toLowerCase(Locale.ROOT));
        return repository.save(rule);
    }

    @Transactional
    public void removeBatchByIds(Collection<Long> ids) {
        repository.deleteAllByIdInBatch(ids);
    }
}
