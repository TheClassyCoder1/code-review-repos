package com.example.lending.ops.part4.members;

import java.util.ArrayList;
import java.util.List;

/** Query criteria for the disbursement order operation history (who changed an order's status, and when). */
public class OmsOrderOperateHistoryExample {

    protected final List<Criterion> criteria = new ArrayList<>();

    public OmsOrderOperateHistoryExample andNoteIsNull() {
        criteria.add(new Criterion("note is null"));
        return this;
    }

    public List<Criterion> getCriteria() { return criteria; }

    public static class Criterion {
        private String condition;
        private boolean noValue;
        private String typeHandler;

        public String getCondition() { return condition; }

        public boolean isNoValue() { return noValue; }

        public String getTypeHandler() { return typeHandler; }

        protected Criterion(String condition) {
            super();
            this.condition = condition;
            this.typeHandler = null;
            this.noValue = true;
        }
    }
}
