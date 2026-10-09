package com.example.lending.loan.servicing.collections.receivables;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** Amount expected from a debtor under a contract or a repayment plan. */
@Entity
@Table(name = "servicing_receivables", schema = "lending")
public class Receivable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "no", nullable = false, unique = true)
    private String no;
    @Column(name = "debtor_id", nullable = false)
    private String debtorId;
    @Column(name = "contract_id")
    private Long contractId;
    @Column(name = "plan_id")
    private Long planId;
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNo() { return no; }
    public void setNo(String no) { this.no = no; }
    public String getDebtorId() { return debtorId; }
    public void setDebtorId(String debtorId) { this.debtorId = debtorId; }
    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
