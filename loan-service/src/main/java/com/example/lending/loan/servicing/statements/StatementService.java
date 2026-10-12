package com.example.lending.loan.servicing.statements;

import com.example.lending.loan.servicing.common.ServicingException;
import com.example.lending.loan.servicing.statements.model.Statement;
import com.example.lending.loan.servicing.statements.model.StatementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/** Statement lookups and publication. */
@Service
@Transactional(readOnly = true)
public class StatementService {

    private final StatementRepository repository;

    public StatementService(StatementRepository repository) {
        this.repository = repository;
    }

    public Statement get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ServicingException.notFound("Statement " + id + " not found"));
    }

    public Statement getForBorrower(Long id, Long borrowerId) {
        return repository.findByIdAndBorrowerId(id, borrowerId)
                .filter(statement -> Statement.Status.PUBLISHED.name().equals(statement.getStatus()))
                .orElseThrow(() -> ServicingException.notFound("Statement " + id + " not found"));
    }

    public List<Statement> publishedForBorrower(Long borrowerId) {
        return repository.findByBorrowerIdAndStatusOrderByPeriodDesc(borrowerId, Statement.Status.PUBLISHED.name());
    }

    @Transactional
    public void publish(Long id) {
        Statement statement = get(id);
        if (!Statement.Status.RENDERED.name().equals(statement.getStatus())) {
            throw ServicingException.conflict("Statement " + id + " is not rendered yet");
        }
        statement.setStatus(Statement.Status.PUBLISHED.name());
        statement.setPublishedAt(Instant.now());
    }
}
