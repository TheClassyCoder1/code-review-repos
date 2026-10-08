package com.example.lending.loan.ops.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration
public class OpsConsoleSecurityConfig {

    static final String USER_ROLE = "USER";
    private static final String[] BY_PASS_URLS = {"/ops/signin", "/ops/assets/**"};

    @Bean
    @Order(99)
    public SecurityFilterChain opsSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/ops/**");
        http.csrf(csrf -> csrf.disable());
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));
        http.authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests
            .requestMatchers(BY_PASS_URLS).permitAll().anyRequest().hasAnyRole(USER_ROLE));
        http.formLogin(formLogin -> formLogin.loginPage("/ops/signin").defaultSuccessUrl("/ops/", true)
            .permitAll().failureUrl("/ops/signin?error"));
        http.httpBasic(Customizer.withDefaults());
        http.logout(logout -> logout.logoutUrl("/ops/logout").invalidateHttpSession(true)
            .clearAuthentication(true).logoutSuccessUrl("/ops/signin?logout"));
        http.exceptionHandling(exceptionHandling -> exceptionHandling
            .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/ops/signin")));
        return http.build();
    }
}
