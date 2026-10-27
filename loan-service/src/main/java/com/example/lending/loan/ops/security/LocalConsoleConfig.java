package com.example.lending.loan.ops.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Local developer profile only: runs the console access walkthrough against a throwaway
 * in-memory fixture account that exists nowhere else.
 */
@Configuration
@Profile("local")
public class LocalConsoleConfig {

    @Bean
    public ConsoleAccessSelfCheck consoleAccessSelfCheck(ConsoleReportService reportService) {
        InMemoryUserDetailsManager fixtureUsers = new InMemoryUserDetailsManager(
                User.withUsername("joe.coder").password("{noop}password").authorities("ROLE_USER", "read").build());
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(fixtureUsers);
        return new ConsoleAccessSelfCheck(new ProviderManager(provider), reportService);
    }
}
