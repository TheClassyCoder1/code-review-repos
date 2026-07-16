package com.example.lending.loan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Core lending service. Code-review test fixture.
 *
 * Scenario anchors:
 *  - REST: /api/v1/loans, /api/v1/loans/{id}, /api/v1/risk-proxy/{id} (server-side proxy),
 *          plus same-path traps /api/v1/health, /api/v1/status
 *  - HTTP clients -> risk-service: RiskClient (Feign), RiskWebClient (WebClient),
 *          LoanCallbackClient (RestTemplate, with retry + circuit breaker)
 *  - Kafka: broker-a (loan.applied TRUE, risk.assessed consumer) + broker-b (loan.applied FALSE match)
 *  - gRPC client -> risk-service RiskService/Assess
 *  - DB: db-primary schema "lending" (loans, audit_log)
 */
@SpringBootApplication
@EnableFeignClients
public class LoanServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LoanServiceApplication.class, args);
    }
}
