package com.example.lending.loan.servicing.statements.portal;

import com.example.lending.loan.servicing.security.portal.CurrentBorrower;
import com.example.lending.loan.servicing.statements.StatementService;
import com.example.lending.loan.servicing.statements.archive.StatementArchiveService;
import com.example.lending.loan.servicing.statements.feed.Feed;
import com.example.lending.loan.servicing.statements.feed.StatementPage.StatementAttachmentPage;
import com.example.lending.loan.servicing.statements.feed.StatementFeedContext;
import com.example.lending.loan.servicing.statements.feed.StatementFeedGenerator;
import com.example.lending.loan.servicing.statements.feed.StatementPage;
import com.example.lending.loan.servicing.statements.model.Statement;
import com.example.lending.loan.servicing.common.ServicingResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Statements of the signed-in borrower. */
@RestController
@RequestMapping("/servicing/portal/statements")
public class StatementPortalController {

    private final StatementService statementService;
    private final StatementArchiveService archiveService;
    private final StatementFeedGenerator feedGenerator;
    private final CurrentBorrower currentBorrower;
    private final String portalBaseUrl;

    public StatementPortalController(StatementService statementService, StatementArchiveService archiveService,
                                     StatementFeedGenerator feedGenerator, CurrentBorrower currentBorrower,
                                     @Value("${servicing.portal.base-url}") String portalBaseUrl) {
        this.statementService = statementService;
        this.archiveService = archiveService;
        this.feedGenerator = feedGenerator;
        this.currentBorrower = currentBorrower;
        this.portalBaseUrl = portalBaseUrl;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('statement:read')")
    public ServicingResult<List<StatementSummary>> list() {
        return ServicingResult.ok(statementService.publishedForBorrower(currentBorrower.borrowerId()).stream()
                .map(StatementSummary::of).toList());
    }

    @GetMapping("/{id}/document")
    @PreAuthorize("hasAuthority('statement:read')")
    public ResponseEntity<byte[]> document(@PathVariable Long id) throws IOException {
        Statement statement = statementService.getForBorrower(id, currentBorrower.borrowerId());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"statement-" + statement.getPeriod() + ".pdf\"")
                .body(archiveService.fetch(statement));
    }

    @GetMapping(value = "/feed", produces = "application/rss+xml")
    @PreAuthorize("hasAuthority('statement:read')")
    public String feed() {
        List<StatementPage> pages = new ArrayList<>();
        for (Statement statement : statementService.publishedForBorrower(currentBorrower.borrowerId())) {
            String title = "Statement " + statement.getPeriod();
            String summary = "Closing balance " + statement.getClosingBalance().toPlainString();
            String name = String.valueOf(statement.getId());
            pages.add(statement.getArchiveKey() != null
                    ? new StatementAttachmentPage(name, statement.getVersion(), statement.getPublishedAt(), title, summary)
                    : new StatementPage(name, statement.getVersion(), statement.getPublishedAt(), title, summary));
        }
        StatementFeedContext context = new StatementFeedContext("My statements", portalBaseUrl, Map.of());
        return feedGenerator.generateStatementFeed(context, pages, new Feed());
    }

    public record StatementSummary(Long id, String period, String closingBalance, int version) {

        static StatementSummary of(Statement statement) {
            return new StatementSummary(statement.getId(), statement.getPeriod(),
                    statement.getClosingBalance().toPlainString(), statement.getVersion());
        }
    }
}
