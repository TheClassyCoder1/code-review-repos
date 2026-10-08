package com.example.lending.loan.webhook;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface PartnerWebhookRepository extends JpaRepository<PartnerWebhook, Long> {

    List<PartnerWebhook> findByPartnerId(String partnerId);

    Optional<PartnerWebhook> findByIdAndPartnerId(Long id, String partnerId);
}
