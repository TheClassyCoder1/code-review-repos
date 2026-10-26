package com.example.lending.ops.part3.webhook;

import com.example.lending.ops.part3.support.*;
import com.example.lending.ops.part3.webhook.WebhookModel.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.*;
import java.util.*;

/** Only write path for webhook channel configs: a config is stored only when its supplier is a supported robot type and it carries a token, so every active config read back maps to a sender that can be registered. */
@Service
@RequiredArgsConstructor
public class WebhookConfigService {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    public void save(String bizCode, String supplier, String sign, String tokenId) {
        boolean supported = Arrays.stream(OaType.values()).anyMatch(t -> StrUtil.equalsAnyIgnoreCase(t.toString(), supplier));
        if (!supported || !StrUtil.isNotBlank(tokenId)) throw new IllegalArgumentException("unsupported or incomplete webhook channel for " + bizCode);
        String value = new Gson().toJson(Map.of("supplier", supplier, "sign", Objects.toString(sign, ""), "tokenId", tokenId));
        jdbcTemplate.update("INSERT INTO sys_system_config (config_type, config_key, config_value, config_status) VALUES (:t, :k, :v, :s) "
                + "ON CONFLICT (config_type, config_key) DO UPDATE SET config_value = :v",
                Map.of("t", SystemConfigTypeEnum.WEBHOOK.getValue(), "k", bizCode, "v", value, "s", YesNoEnum.YES.getCode()));
    }
    @Repository
    @RequiredArgsConstructor
    public static class SysSystemConfigMapper {
        private final NamedParameterJdbcTemplate jdbcTemplate;
        public List<SysSystemConfigEntity> selectList(LambdaQuery<SysSystemConfigEntity> query) {
            return jdbcTemplate.query("SELECT * FROM sys_system_config", new BeanPropertyRowMapper<>(SysSystemConfigEntity.class)).stream().filter(query).toList();
        }
    }
}
