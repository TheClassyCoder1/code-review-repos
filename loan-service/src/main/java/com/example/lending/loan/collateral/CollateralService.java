package com.example.lending.loan.collateral;

import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.repository.LoanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class CollateralService {

    private final CollateralAppraisalParser parser;
    private final LoanRepository loanRepository;

    public CollateralService(CollateralAppraisalParser parser, LoanRepository loanRepository) {
        this.parser = parser;
        this.loanRepository = loanRepository;
    }

    public CollateralSummary importAppraisal(Long loanId, InputStream xml) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan not found"));
        CollateralAppraisalParser.Appraisal appraisal = parser.parse(xml);
        BigDecimal loanToValue = BigDecimal.valueOf(loan.getAmount())
                .divide(appraisal.appraisedValue(), 4, RoundingMode.HALF_EVEN);
        return new CollateralSummary(loanId, appraisal.propertyId(), appraisal.appraisedValue(),
                appraisal.appraisalDate(), loanToValue);
    }

    public record CollateralSummary(Long loanId, String propertyId, BigDecimal appraisedValue,
                                    LocalDate appraisalDate, BigDecimal loanToValue) {
    }
}
