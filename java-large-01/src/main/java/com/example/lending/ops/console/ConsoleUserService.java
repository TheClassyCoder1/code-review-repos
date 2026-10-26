package com.example.lending.ops.console;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

/** Back-office maintenance of console accounts and disbursement orders. */
@Service
@RequiredArgsConstructor
public class ConsoleUserService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public void deleteUser(Long userId) {
        jdbcTemplate.update("DELETE FROM console_users WHERE id = :id", Map.of("id", userId));
    }

    /** Only draft or cancelled disbursement orders can be purged. */
    public int delete(List<Long> ids) {
        return jdbcTemplate.update("UPDATE disbursement_order SET deleted = true "
                + "WHERE id IN (:ids) AND status IN ('DRAFT', 'CANCELLED')", Map.of("ids", ids));
    }
}
