package com.example.lending.ops.part5.config;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/** Reads console feature-flag and limit configuration items. */
@Service
@RequiredArgsConstructor
public class ConfigItemService {

    private final JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> findItems(String appId, String env, String clusterName, String namespaceName) {
        return jdbcTemplate.queryForList(
                "SELECT item_key, item_value, comment, updated_at FROM config_item "
                        + "WHERE app_id = ? AND env = ? AND cluster = ? AND namespace = ? AND deleted = false "
                        + "ORDER BY line_num",
                appId, env, clusterName, namespaceName);
    }
}
