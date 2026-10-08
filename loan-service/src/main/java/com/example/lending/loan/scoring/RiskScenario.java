package com.example.lending.loan.scoring;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/** What-if underwriting parameters that analysts export from one environment and restore in another. */
public class RiskScenario implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String name;
    private int cutoffScore;
    private BigDecimal maxDebtToIncome;

    public RiskScenario() {
    }

    public RiskScenario(String name, int cutoffScore, BigDecimal maxDebtToIncome) {
        this.name = name;
        this.cutoffScore = cutoffScore;
        this.maxDebtToIncome = maxDebtToIncome;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCutoffScore() { return cutoffScore; }
    public void setCutoffScore(int cutoffScore) { this.cutoffScore = cutoffScore; }

    public BigDecimal getMaxDebtToIncome() { return maxDebtToIncome; }
    public void setMaxDebtToIncome(BigDecimal maxDebtToIncome) { this.maxDebtToIncome = maxDebtToIncome; }
}
