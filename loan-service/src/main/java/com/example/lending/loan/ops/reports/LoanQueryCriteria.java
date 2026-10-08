package com.example.lending.loan.ops.reports;

import java.util.ArrayList;
import java.util.List;

/** Criteria builder for ad-hoc loan report queries. */
public class LoanQueryCriteria {

    private final List<Criterion> criteria = new ArrayList<>();

    public List<Criterion> getCriteria() {
        return criteria;
    }

    public LoanQueryCriteria andStatusEqualTo(String value) {
        criteria.add(new Criterion("status =", value));
        return this;
    }

    public LoanQueryCriteria andAmountBetween(double value1, double value2) {
        criteria.add(new Criterion("amount between", value1, value2));
        return this;
    }

    public static class Criterion {
        private final String condition;
        private Object value;
        private Object secondValue;
        private boolean singleValue;
        private boolean betweenValue;
        private final String typeHandler;

        protected Criterion(String condition, Object value) {
            this(condition, value, null);
        }

        protected Criterion(String condition, Object value, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.typeHandler = typeHandler;
            this.singleValue = true;
        }

        protected Criterion(String condition, Object value, Object secondValue) {
            this(condition, value, secondValue, null);
        }

        protected Criterion(String condition, Object value, Object secondValue, String typeHandler) {
            super();
            this.condition = condition;
            this.value = value;
            this.secondValue = secondValue;
            this.typeHandler = typeHandler;
            this.betweenValue = true;
        }

        public String getCondition() { return condition; }
        public Object getValue() { return value; }
        public Object getSecondValue() { return secondValue; }
        public boolean isSingleValue() { return singleValue; }
        public boolean isBetweenValue() { return betweenValue; }
        public String getTypeHandler() { return typeHandler; }
    }
}
