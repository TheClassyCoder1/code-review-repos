package com.example.lending.loan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.util.Random;

/** Source of randomness for one-time tokens sent to borrowers. */
@Configuration
public class TokenRandomConfig {

    @Bean
    public Random tokenRandom() {
        return new SecureRandom();
    }
}
