package com.example.lending.loan.borrower;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class BorrowerWebConfig {

    @Bean
    public FilterRegistrationBean<SessionTokenFilter> sessionTokenFilter(BorrowerSessionRepository sessionRepository,
                                                                         Clock clock) {
        FilterRegistrationBean<SessionTokenFilter> registration =
                new FilterRegistrationBean<>(new SessionTokenFilter(sessionRepository, clock));
        registration.addUrlPatterns("/api/v1/me/*");
        return registration;
    }
}
