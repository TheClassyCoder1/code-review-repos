package com.example.lending.ops.part4.members;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/** A loyalty task (e.g. "first on-time repayment") that awards growth and points to a borrower. */
@Getter
@Setter
public class UmsMemberTask implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private Integer growth;
    private Integer intergration;
    /** 0 = onboarding task, 1 = recurring task. */
    private Integer type;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", name=").append(name);
        sb.append(", growth=").append(growth);
        sb.append(", intergration=").append(intergration);
        sb.append(", type=").append(type);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}
