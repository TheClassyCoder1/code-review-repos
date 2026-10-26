package com.example.lending.ops.expense;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class OaTravelReimbursementService {

    /** Travel expense claim filed by a field agent after a borrower site visit. */
    @Data
    public static class OaTravelReimbursementDO {
        private Long id;
        private String applicantUserId, tenantId, loanReference, status;
        private LocalDate travelDate;
        private BigDecimal amount;
    }

    public record OaTravelReimbursementRespVO(Long id, String applicantUserId, String loanReference,
                                              LocalDate travelDate, BigDecimal amount, String status) { }

    private final JdbcTemplate jdbcTemplate;

    public OaTravelReimbursementDO getTravelReimbursement(Long id) {
        return jdbcTemplate.query("SELECT * FROM oa_travel_reimbursement WHERE id = ?",
                new BeanPropertyRowMapper<>(OaTravelReimbursementDO.class), id).stream().findFirst().orElse(null);
    }
}
