package com.example.lending.loan.integration.kyc;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class IntegrationSettings {

    public static final String CONFIG_IGNORE_BAD_SSL = "integration.kyc.ignore-bad-ssl";

    private final Environment environment;

    public IntegrationSettings(Environment environment) {
        this.environment = environment;
    }

    public boolean getBool(String key, boolean defaultValue) {
        if (CONFIG_IGNORE_BAD_SSL.equals(key) && !environment.acceptsProfiles(Profiles.of("local"))) {
            return false;
        }
        return environment.getProperty(key, Boolean.class, defaultValue);
    }
}
