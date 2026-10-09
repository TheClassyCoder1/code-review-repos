package com.example.lending.loan.servicing.collections.plans;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Batch operations on plan instalments. */
public interface PlanItemMapper extends JpaRepository<PlanItem, Long> {

    List<PlanItem> findByPlanIdOrderByDueDate(Long planId);

    default List<PlanItem> selectListByPlanId(Long planId) {
        return findByPlanIdOrderByDueDate(planId);
    }

    default void insertBatch(List<PlanItem> items) {
        saveAll(items);
    }

    default void deleteByIds(List<Long> ids) {
        deleteAllByIdInBatch(ids);
    }

    /** Copies the schedule fields onto the stored instalments; payment progress is kept. */
    default void updateBatch(List<PlanItem> items) {
        Map<Long, PlanItem> stored = findAllById(items.stream().map(PlanItem::getId).toList())
                .stream().collect(Collectors.toMap(PlanItem::getId, Function.identity()));
        for (PlanItem item : items) {
            PlanItem target = stored.get(item.getId());
            if (target != null && target.getPlanId().equals(item.getPlanId())) {
                target.setDueDate(item.getDueDate());
                target.setAmount(item.getAmount());
            }
        }
        saveAll(stored.values());
    }
}
