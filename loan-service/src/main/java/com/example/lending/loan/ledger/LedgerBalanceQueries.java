package com.example.lending.loan.ledger;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class LedgerBalanceQueries {

    private final JdbcTemplate jdbcTemplate;

    public LedgerBalanceQueries(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public BigDecimal sumBalance(LedgerPartition partition) {
        return jdbcTemplate.queryForObject(getBalanceWithConditionSql(partition), BigDecimal.class);
    }

    protected String getListBalanceSql(LedgerRegion region) {
        return "SELECT COALESCE(SUM(amount), 0) FROM lending.ledger_entries WHERE region = '" + region.code() + "'";
    }

    protected String getBalanceWithConditionSql(LedgerPartition partition) {
        return String.format(
                getListBalanceSql(partition.region())
                        + "  and  branch_code = '%s' and product_code = '%s'",
                partition.branchCode(),
                partition.productCode());
    }
}
