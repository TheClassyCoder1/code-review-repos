package com.example.lending.loan.servicing.statements.archive;

import com.example.lending.loan.servicing.common.ServicingException;
import com.example.lending.loan.servicing.statements.model.Statement;
import com.example.lending.loan.servicing.statements.model.StatementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;

/** Stores rendered statement PDFs in the archive and reads them back. The archive directory is written only here. */
@Service
public class StatementArchiveService {

    static final int MAX_STATEMENT_BYTES = 5 * 1024 * 1024;

    private final SftpStatementArchive archive;
    private final StatementRepository statements;

    public StatementArchiveService(SftpStatementArchive archive, StatementRepository statements) {
        this.archive = archive;
        this.statements = statements;
    }

    @Transactional
    public String store(Long statementId, InputStream document) throws IOException {
        byte[] pdf = document.readNBytes(MAX_STATEMENT_BYTES + 1);
        if (pdf.length == 0 || pdf.length > MAX_STATEMENT_BYTES) {
            throw ServicingException.badRequest("Statement PDF must be between 1 byte and 5 MB");
        }
        Statement statement = statements.findById(statementId)
                .orElseThrow(() -> ServicingException.notFound("Statement " + statementId + " not found"));
        String key = statement.getBorrowerId() + "/" + statement.getPeriod() + "-" + statement.getId()
                + "-v" + statement.getVersion() + ".pdf";
        archive.upload(key, pdf);
        statement.setArchiveKey(key);
        statement.setStatus(Statement.Status.RENDERED.name());
        return key;
    }

    public byte[] fetch(Statement statement) throws IOException {
        if (statement.getArchiveKey() == null) {
            throw ServicingException.notFound("Statement " + statement.getId() + " has no archived document");
        }
        return archive.getContent(statement.getArchiveKey());
    }
}
