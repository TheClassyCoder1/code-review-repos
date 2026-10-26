package com.example.lending.ops.part3.accesskey;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class AccessKeyRepository {
  private final NamedParameterJdbcTemplate jdbcTemplate;
  public long countByAppId(String appId) {
    return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM partner_access_key WHERE app_id = :appId AND deleted = false", Map.of("appId", appId), Long.class);
  }
  public AccessKey save(AccessKey entity) {
    GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update("INSERT INTO partner_access_key (app_id, secret, enabled, created_by, last_modified_by) "
        + "VALUES (:appId, :secret, :enabled, :dataChangeCreatedBy, :dataChangeLastModifiedBy)",
        new BeanPropertySqlParameterSource(entity), keyHolder, new String[] {"id"});
    entity.setId(keyHolder.getKey().longValue());
    return entity;
  }
}
