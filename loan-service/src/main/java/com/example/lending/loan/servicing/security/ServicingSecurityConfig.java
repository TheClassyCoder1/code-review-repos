package com.example.lending.loan.servicing.security;

import com.example.lending.loan.servicing.security.portal.PortalTokenAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Security for the servicing back-office ({@code /servicing/**}). Existing {@code /api/v1} routes are not
 * matched by these chains and keep their current behaviour.
 */
@Configuration
@EnableMethodSecurity
public class ServicingSecurityConfig {

    @Bean
    public PasswordEncoder servicingPasswordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public FilterRegistrationBean<PortalTokenAuthenticationFilter> portalTokenFilterRegistration(
            PortalTokenAuthenticationFilter filter) {
        FilterRegistrationBean<PortalTokenAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /** Borrower statement portal: stateless, authenticated by the portal access token header. */
    @Bean
    @Order(1)
    public SecurityFilterChain portalSecurityFilterChain(HttpSecurity http,
                                                         PortalTokenAuthenticationFilter portalTokenFilter) throws Exception {
        http.securityMatcher("/servicing/portal/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/servicing/portal/auth/**").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .addFilterBefore(portalTokenFilter, BasicAuthenticationFilter.class);
        return http.build();
    }

    /** Operator back-office: session based. */
    @Bean
    @Order(2)
    public SecurityFilterChain operatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/servicing/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/servicing/auth/**").permitAll()
                        .requestMatchers("/servicing/sso/signed-out").permitAll()
                        .requestMatchers("/servicing/settlement/callbacks/**").permitAll()
                        .requestMatchers("/servicing/admin/roles/**").hasRole("SECURITY_ADMIN")
                        .requestMatchers("/servicing/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/servicing/settlement/callbacks/**"))
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
