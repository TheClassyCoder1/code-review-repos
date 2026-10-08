package com.example.lending.loan.notification;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Key/value settings shared by all loan-service instances. */
@Repository
public class SettingStore {

    private final JdbcTemplate jdbc;

    public SettingStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public String get(String key) {
        return jdbc.queryForList("SELECT setting_value FROM lending.app_settings WHERE setting_key = ?",
                String.class, key).stream().findFirst().orElse(null);
    }

    public void set(String key, String value) {
        jdbc.update("INSERT INTO lending.app_settings (setting_key, setting_value) VALUES (?, ?) "
                + "ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value", key, value);
    }
}
