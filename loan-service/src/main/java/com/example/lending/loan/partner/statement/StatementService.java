package com.example.lending.loan.partner.statement;

import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class StatementService {

    private final LoanStatementRepository statementMapper;

    public StatementService(LoanStatementRepository statementMapper) {
        this.statementMapper = statementMapper;
    }

    public LoanStatement getBorrowerStatement(Long id, Long borrowerId) {
        return validateStatementByBorrowerId(id, borrowerId);
    }

    private LoanStatement validateStatementByBorrowerId(Long id, Long borrowerId) {
        LoanStatement statement = statementMapper.findById(id).orElse(null);
        if (statement == null || !Objects.equals(statement.getBorrowerId(), borrowerId)) {
            throw new StatementNotFoundException();
        }
        return statement;
    }
}
