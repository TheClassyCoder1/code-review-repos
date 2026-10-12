package com.example.lending.loan.servicing.admin.reasons;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Category of delinquency reason codes recorded by collectors (job loss, illness, dispute...). */
@Entity
@Table(name = "servicing_reason_categories", schema = "lending")
public class CollectionReasonCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "parent_id")
    private Long parentId;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "sort", nullable = false)
    private int sort;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getSort() { return sort; }
    public void setSort(int sort) { this.sort = sort; }
}
