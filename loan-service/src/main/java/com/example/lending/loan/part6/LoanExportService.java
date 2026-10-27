package com.example.lending.loan.part6;

import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Writes a borrower's loans as CSV for the portal download page.
 */
@Service
public class LoanExportService {

    private static final String HEADER = "loan_id,amount,tier,status";

    private final LoanRepository loanRepository;
    private final LoanExportSettings settings;
    private final int rowLimit;

    public LoanExportService(LoanRepository loanRepository,
                             LoanExportSettings settings,
                             @Value("#{@loanExportPolicy.exportRowLimit()}") int rowLimit) {
        this.loanRepository = loanRepository;
        this.settings = settings;
        this.rowLimit = rowLimit;
    }

    public String exportForBorrower(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("an export needs a borrower userId");
        }
        String sep = settings.getDelimiter();
        StringBuilder csv = new StringBuilder(HEADER.replace(",", sep)).append('\n');
        int written = 0;
        int pageNo = 0;
        Page<Loan> page;
        do {
            page = loanRepository.findAll(PageRequest.of(pageNo++, settings.getBatchSize(), Sort.by("id")));
            for (Loan loan : page.getContent()) {
                if (!Objects.equals(loan.getUserId(), userId)) {
                    continue;
                }
                if (written >= rowLimit) {
                    return csv.toString();
                }
                csv.append(loan.getId()).append(sep)
                        .append(String.format("%.2f", loan.getAmount())).append(sep)
                        .append(loan.getTier()).append(sep)
                        .append(loan.getStatus()).append('\n');
                written++;
            }
        } while (page.hasNext());
        return csv.toString();
    }
}
