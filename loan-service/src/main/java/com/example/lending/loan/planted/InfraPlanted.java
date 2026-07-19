package com.example.lending.loan.planted;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Additional infra/reliability surface for infra review. */
@Configuration
public class InfraPlanted {

    public static final String PROD_DB = "jdbc:postgresql://prod-db-master:5432/lending";

    @Bean
    public WebClient noTimeoutRiskClient() {
        return WebClient.create("http://risk-service:8082");
    }

    @Bean
    public ExecutorService loanWorkerPool() {
        return Executors.newFixedThreadPool(1);
    }

    @Bean
    public ExecutorService unboundedPool() {
        return Executors.newCachedThreadPool();
    }
}
