package com.example.lending.loan.reporting;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class CollectionBucketService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CollectionBucketService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int update(Long id, String name) {
        if (!StringUtils.hasText(name) || name.length() > 64) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid bucket name");
        }
        return jdbcTemplate.update("update lending.collection_buckets set name = :name where id = :id",
                Map.of("id", id, "name", name.trim()));
    }

    @Transactional
    public void deleteBucketsAndRules(Long[] ids) {
        if (ids == null || ids.length == 0) {
            return;
        }
        Map<String, Object> params = Map.of("ids", Arrays.asList(ids));
        jdbcTemplate.update("delete from lending.collection_bucket_rules where bucket_id in (:ids)", params);
        jdbcTemplate.update("delete from lending.collection_buckets where id in (:ids)", params);
    }
}
