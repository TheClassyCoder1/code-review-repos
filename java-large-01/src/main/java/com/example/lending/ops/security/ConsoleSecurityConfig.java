package com.example.lending.ops.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Session-based chain for the browser console used by ops staff. */
@Configuration
public class ConsoleSecurityConfig {
    /** Documents the permission a console handler needs; the matching rule is declared below. */
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RequiresPermissions {
        String value();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain consoleSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/console/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/console/orders/**").hasRole("OPS_ADMIN")
                        .requestMatchers("/console/cdc/**", "/console/backup/**").hasRole("PLATFORM_ADMIN")
                        .requestMatchers("/console/s/deleteUser").hasAuthority("user:delete")
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/console/login").permitAll());
        return http.build();
    }
}
