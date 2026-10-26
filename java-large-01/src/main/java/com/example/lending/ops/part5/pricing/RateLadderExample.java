package com.example.lending.ops.part5.pricing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Query criteria for the {@code rate_ladder} table, which stores the tiered
 * interest discounts applied by loan amount band. Criteria groups are OR-ed.
 */
public class RateLadderExample {

    protected List<Criteria> oredCriteria = new ArrayList<>();

    public List<Criteria> getOredCriteria() {
        return oredCriteria;
    }

    public Criteria or() {
        Criteria criteria = createCriteriaInternal();
        oredCriteria.add(criteria);
        return criteria;
    }

    public Criteria createCriteria() {
        Criteria criteria = createCriteriaInternal();
        if (oredCriteria.isEmpty()) {
            oredCriteria.add(criteria);
        }
        return criteria;
    }

    protected Criteria createCriteriaInternal() {
        return new Criteria();
    }

    public static class Criteria {
        private final List<String> conditions = new ArrayList<>();
        private final List<Object> values = new ArrayList<>();

        protected Criteria() {
        }

        private Criteria add(String condition, Object value, String property) {
            if (value == null) {
                throw new IllegalArgumentException("Value for " + property + " cannot be null");
            }
            conditions.add(condition);
            values.add(value);
            return this;
        }

        public Criteria andProductIdEqualTo(Long value) {
            return add("product_id =", value, "productId");
        }

        public Criteria andMinAmountGreaterThanOrEqualTo(BigDecimal value) {
            return add("min_amount >=", value, "minAmount");
        }
    }
}
