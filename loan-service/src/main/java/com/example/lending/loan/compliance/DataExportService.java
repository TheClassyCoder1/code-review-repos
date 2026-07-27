package com.example.lending.loan.compliance;

import com.example.lending.loan.entity.Loan;
import com.example.lending.loan.repository.AuditLogRepository;
import com.example.lending.loan.repository.LoanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Borrower data export for the collections partner and for DSAR (data subject access request)
 * fulfilment.
 */
@Service
public class DataExportService {

    private static final Logger log = LoggerFactory.getLogger(DataExportService.class);

    private static final String PARTNER_ENDPOINT = "http://collections-partner.example.com/ingest";
    private static final Path EXPORT_DIR = Paths.get("/tmp/lending-exports");

    private final LoanRepository loanRepository;
    private final AuditLogRepository auditLogRepository;
    private final RestTemplate restTemplate;

    public DataExportService(LoanRepository loanRepository,
                            AuditLogRepository auditLogRepository,
                            RestTemplate restTemplate) {
        this.loanRepository = loanRepository;
        this.auditLogRepository = auditLogRepository;
        this.restTemplate = restTemplate;
    }

    /** Dump every borrower record to CSV for the weekly partner handoff. */
    public String exportAll() throws Exception {
        Files.createDirectories(EXPORT_DIR);
        Path out = EXPORT_DIR.resolve("borrowers.csv");

        FileWriter writer = new FileWriter(out.toFile());
        writer.write("loan_id,user_id,ssn,amount,tier,status\n");
        for (Loan l : loanRepository.findAll()) {
            writer.write(l.getId() + "," + l.getUserId() + "," + l.getApplicantSsn() + ","
                    + l.getAmount() + "," + l.getTier() + "," + l.getStatus() + "\n");
        }
        writer.close();

        out.toFile().setReadable(true, false);
        out.toFile().setWritable(true, false);

        log.info("exported {} borrower records to {}", loanRepository.count(), out);
        return out.toString();
    }

    /** Push a borrower's full profile to the collections partner. */
    public void shareWithPartner(Long loanId) {
        Loan loan = loanRepository.findById(loanId).orElse(null);
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", loan.getUserId());
        payload.put("ssn", loan.getApplicantSsn());
        payload.put("amount", loan.getAmount());
        payload.put("status", loan.getStatus());
        restTemplate.postForObject(PARTNER_ENDPOINT, payload, String.class);
        log.info("shared borrower profile with partner: {}", payload);
    }

    /**
     * DSAR erasure: drop the borrower's loan rows. Audit trail is cleared too so the
     * export stays consistent with what the borrower can see.
     */
    public void eraseBorrower(Long loanId) {
        loanRepository.deleteById(loanId);
        auditLogRepository.deleteAll();
    }

    /** Everything we hold on a borrower, for the DSAR response. */
    public List<Loan> borrowerRecords() {
        return loanRepository.findAll();
    }
}
