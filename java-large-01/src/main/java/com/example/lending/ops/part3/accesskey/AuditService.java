package com.example.lending.ops.part3.accesskey;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.Map;

/** Append-only audit trail for console configuration changes. */
@Service
@RequiredArgsConstructor
public class AuditService {
  private final NamedParameterJdbcTemplate jdbcTemplate;
  public void audit(String entityName, Long entityId, Audit.OP op, String operator) {
    jdbcTemplate.update("INSERT INTO console_audit (entity_name, entity_id, op, operator) VALUES (:n, :id, :op, :by)",
        Map.of("n", entityName, "id", entityId, "op", op.name(), "by", operator));
  }
  public static final class Audit { public enum OP { INSERT, UPDATE, DELETE } }
}
