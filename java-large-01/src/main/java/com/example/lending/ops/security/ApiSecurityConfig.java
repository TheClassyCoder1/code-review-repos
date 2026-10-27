package com.example.lending.ops.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** Fallback chain for everything outside /console/**; requests are authenticated by HeaderTokenAuthFilter. */
@Configuration
public class ApiSecurityConfig {
    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
      http.csrf(csrf -> csrf.disable());
      http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));
      return http.build();
    }
}
