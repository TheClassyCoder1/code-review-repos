package com.example.lending.loan.servicing.collections.accounts;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

/** Client for the ledger service that holds repayment accounts. */
@Component
public class RepaymentAccountManager {

    /** Repayment account opened for a debtor at the ledger service. */
    public static class AccountInstance {

        private String productIdentifier;
        private String debtorIdentifier;
        private String accountIdentifier;

        public String getProductIdentifier() { return productIdentifier; }
        public void setProductIdentifier(String productIdentifier) { this.productIdentifier = productIdentifier; }

        public String getDebtorIdentifier() { return debtorIdentifier; }
        public void setDebtorIdentifier(String debtorIdentifier) { this.debtorIdentifier = debtorIdentifier; }

        public String getAccountIdentifier() { return accountIdentifier; }
        public void setAccountIdentifier(String accountIdentifier) { this.accountIdentifier = accountIdentifier; }
    }

    private final RestClient restClient;

    public RepaymentAccountManager(@Value("${servicing.ledger.url}") String ledgerUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .baseUrl(ledgerUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public void create(AccountInstance instance) {
        restClient.post()
                .uri("/ledger/v1/repayment-accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .body(instance)
                .retrieve()
                .toBodilessEntity();
    }

    public void postAccountCommand(String accountIdentifier, String action) {
        restClient.post()
                .uri("/ledger/v1/repayment-accounts/{id}/commands", accountIdentifier)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("action", action))
                .retrieve()
                .toBodilessEntity();
    }
}
