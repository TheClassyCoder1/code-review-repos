package com.example.lending.loan.portal;

import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.notification.Notice;
import com.example.lending.loan.notification.NoticeStore;
import com.example.lending.loan.service.LoanService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Borrower portal access to rendered loan documents. */
@RestController
public class DocumentProxyController {

    private final LoanService loanService;
    private final NoticeStore noticeStore;
    private final DocumentProxyService proxyService;

    public DocumentProxyController(LoanService loanService, NoticeStore noticeStore, DocumentProxyService proxyService) {
        this.loanService = loanService;
        this.noticeStore = noticeStore;
        this.proxyService = proxyService;
    }

    @GetMapping("/api/v1/portal/documents/{type}/{id}")
    public void proxyGet(@PathVariable String type, @PathVariable Long id,
                         HttpServletRequest request, HttpServletResponse response) throws Exception {
        proxy(type, request, response, id);
    }

    private void proxy(String type, HttpServletRequest request, HttpServletResponse response,
                       Long id) throws Exception {
        Notice notice;
        switch (type) {
            case "agreement":
                Loan agreementLoan = requireLoan(id);
                checkProxyOwner(request, agreementLoan.getUserId());
                proxyService.proxy(response, "loans/" + agreementLoan.getId() + "/agreement");
                return;
            case "schedule":
                Loan scheduleLoan = requireLoan(id);
                checkProxyOwner(request, scheduleLoan.getUserId());
                proxyService.proxy(response, "loans/" + scheduleLoan.getId() + "/schedule");
                return;
            case "statement_batch":
                proxyService.proxyStatementBatch(request, response, id);
                return;
            case "notice":
                notice = noticeStore.getById(id);
                checkProxyOwner(request, notice.userId());
                proxyService.proxy(response, "notices/" + notice.id());
                return;
            case "receipt":
                notice = noticeStore.getById(id);
                checkProxyOwner(request, notice.userId());
                proxyService.proxy(response, "notices/" + notice.id() + "/receipt");
                return;
            default:
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private Loan requireLoan(Long id) {
        Loan loan = loanService.getLoan(id);
        if (loan == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return loan;
    }

    private void checkProxyOwner(HttpServletRequest request, Long ownerUserId) {
        if (!DocumentProxyService.callerId(request).equals(ownerUserId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
