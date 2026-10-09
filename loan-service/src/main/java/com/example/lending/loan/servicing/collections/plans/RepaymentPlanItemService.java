package com.example.lending.loan.servicing.collections.plans;

import com.example.lending.loan.servicing.common.CollectionDiffs;
import com.example.lending.loan.servicing.common.ServicingException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Edits the instalments of a negotiated repayment plan. */
@Service
public class RepaymentPlanItemService {

    public record PlanItemSaveReq(Long id, @NotNull LocalDate dueDate, @NotNull @Positive BigDecimal amount) {

        PlanItem toEntity() {
            PlanItem item = new PlanItem();
            item.setId(id);
            item.setDueDate(dueDate);
            item.setAmount(amount);
            return item;
        }
    }

    private static final int MAX_ITEMS = 120;

    private final PlanItemMapper planItemMapper;

    public RepaymentPlanItemService(PlanItemMapper planItemMapper) {
        this.planItemMapper = planItemMapper;
    }

    @Transactional(readOnly = true)
    public List<PlanItem> getPlanItems(Long planId) {
        return planItemMapper.selectListByPlanId(planId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updatePlanItemList(Long planId, List<PlanItemSaveReq> items) {
        validatePlanItemList(items);

        List<PlanItem> oldList = planItemMapper.selectListByPlanId(planId);
        List<PlanItem> newList = items.stream().map(PlanItemSaveReq::toEntity).toList();
        List<List<PlanItem>> diffList = CollectionDiffs.diffList(oldList, newList,
                (oldVal, newVal) -> Objects.equals(oldVal.getId(), newVal.getId()));

        if (!diffList.get(2).isEmpty()) {
            List<Long> deleteItemIds = diffList.get(2).stream().map(PlanItem::getId).toList();
            validatePlanItemsUnused(diffList.get(2));
            planItemMapper.deleteByIds(deleteItemIds);
        }
        if (!diffList.get(0).isEmpty()) {
            diffList.get(0).forEach(item -> {
                item.setPlanId(planId);
                item.setId(null);
            });
            planItemMapper.insertBatch(diffList.get(0));
        }
        if (!diffList.get(1).isEmpty()) {
            diffList.get(1).forEach(item -> item.setPlanId(planId));
            planItemMapper.updateBatch(diffList.get(1));
        }
    }

    private void validatePlanItemList(List<PlanItemSaveReq> items) {
        if (items == null || items.isEmpty()) {
            throw ServicingException.badRequest("A repayment plan needs at least one instalment");
        }
        if (items.size() > MAX_ITEMS) {
            throw ServicingException.badRequest("A repayment plan can have at most " + MAX_ITEMS + " instalments");
        }
        Set<LocalDate> dueDates = new HashSet<>();
        for (PlanItemSaveReq item : items) {
            if (!dueDates.add(item.dueDate())) {
                throw ServicingException.badRequest("Two instalments fall on " + item.dueDate());
            }
        }
    }

    private void validatePlanItemsUnused(List<PlanItem> items) {
        for (PlanItem item : items) {
            if (item.getPaidAmount() != null && item.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
                throw ServicingException.conflict("Instalment due " + item.getDueDate() + " already has payments");
            }
        }
    }
}
