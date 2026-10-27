package com.example.lending.loan.document.export;

import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.repository.LoanRepository;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;

@Service
public class LoanExportService {

    private final LoanRepository loanRepository;
    private final ObjectMapper objectMapper;

    public LoanExportService(LoanRepository loanRepository, ObjectMapper objectMapper) {
        this.loanRepository = loanRepository;
        this.objectMapper = objectMapper;
    }

    /** Streams the export and always closes {@code out}, also when writing fails. */
    @Transactional(readOnly = true)
    public void writeLoans(LoanExportParams params, OutputStream out) throws IOException {
        try (JsonGenerator generator = objectMapper.getFactory().createGenerator(out)) {
            generator.enable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
            generator.writeStartArray();
            for (Loan loan : loanRepository.findAll()) {
                if (params.getStatus() == null || params.getStatus().equals(loan.getStatus())) {
                    generator.writeStartObject();
                    generator.writeNumberField("id", loan.getId());
                    generator.writeNumberField("amount", loan.getAmount());
                    generator.writeStringField("tier", loan.getTier());
                    generator.writeStringField("status", loan.getStatus());
                    generator.writeEndObject();
                }
            }
            generator.writeEndArray();
        }
    }
}
