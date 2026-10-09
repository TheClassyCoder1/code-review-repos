package com.example.lending.loan.servicing.settlement.callbacks;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.util.List;

/** Registers the callback token filter in front of the callback endpoints. */
@Configuration
@EnableConfigurationProperties(SettlementCallbackConfig.SettlementCallbackSettings.class)
public class SettlementCallbackConfig {

    /** Access control of the partner settlement callback endpoints. */
    @ConfigurationProperties(prefix = "servicing.settlement.callbacks")
    public record SettlementCallbackSettings(boolean accessControlEnabled, List<String> accessTokens) {

        public SettlementCallbackSettings {
            accessTokens = accessTokens == null ? List.of() : List.copyOf(accessTokens);
        }

        public boolean isCallbackAccessControlEnabled() {
            return accessControlEnabled;
        }
    }

    @Bean
    public FilterRegistrationBean<SettlementCallbackAuthenticationFilter> settlementCallbackFilter(
            SettlementCallbackSettings settings) {
        FilterRegistrationBean<SettlementCallbackAuthenticationFilter> registration =
                new FilterRegistrationBean<>(new SettlementCallbackAuthenticationFilter(settings));
        registration.addUrlPatterns("/servicing/settlement/callbacks/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
