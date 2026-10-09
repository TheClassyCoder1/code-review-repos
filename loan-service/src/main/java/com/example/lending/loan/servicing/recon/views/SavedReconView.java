package com.example.lending.loan.servicing.recon.views;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Named filter set an operator saved on the reconciliation workbench. */
@Entity
@Table(name = "recon_saved_views", schema = "lending")
public class SavedReconView {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;
    @Column(name = "operator_id", nullable = false)
    private String operatorId;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "filter_json", nullable = false, length = 4000)
    private String filterJson;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOperatorId() { return operatorId; }
    public void setOperatorId(String operatorId) { this.operatorId = operatorId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFilterJson() { return filterJson; }
    public void setFilterJson(String filterJson) { this.filterJson = filterJson; }
}
