package com.example.lending.loan.servicing.admin.reasons;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Editable fields of a reason category. */
public class ReasonCategoryParam {

    private Long parentId;

    @NotBlank
    @Size(max = 64)
    private String name;

    @Min(0)
    private int sort;

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getSort() { return sort; }
    public void setSort(int sort) { this.sort = sort; }

}
