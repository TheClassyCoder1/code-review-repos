package com.example.lending.loan.servicing.settlement.partners;

import com.example.lending.loan.servicing.common.Digests;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/** Issues API keys to settlement partners. A new key replaces the existing key of the same type. */
@Service
public class PartnerApiKeyService {

    /** Newly issued key: the stored metadata and the plain-text key, which is returned exactly once. */
    public record ApiKeyCreationResult(PartnerApiKey apiKey, String plainTextKey) {
    }

    private final SettlementPartnerRepository persistenceService;
    private final TransactionTemplate executionContextManager;

    public PartnerApiKeyService(SettlementPartnerRepository persistenceService, TransactionTemplate transactionTemplate) {
        this.persistenceService = persistenceService;
        this.executionContextManager = transactionTemplate;
    }

    public ApiKeyCreationResult generateApiKeyWithType(String partnerId, PartnerApiKey.ApiKeyType keyType, Long validityPeriod) {
        return executionContextManager.execute(status -> {
            String plainTextKey = PartnerApiKey.generatePlainTextKey();

            PartnerApiKey apiKey = new PartnerApiKey();
            apiKey.setItemId(UUID.randomUUID().toString());
            apiKey.setKeyHash(Digests.sha256Hex(plainTextKey));
            apiKey.setMaskedKey(PartnerApiKey.maskPlainTextKey(plainTextKey));
            apiKey.setKeyType(keyType);
            apiKey.setCreationDate(new Date());
            if (validityPeriod != null) {
                apiKey.setExpirationDate(new Date(System.currentTimeMillis() + validityPeriod));
            }

            SettlementPartner partner = persistenceService.findById(partnerId).orElse(null);
            if (partner != null) {
                // Remove any existing key of the same type
                if (partner.getApiKeys() == null) {
                    partner.setApiKeys(new ArrayList<>());
                }
                partner.getApiKeys().removeIf(existingKey -> existingKey.getKeyType() == keyType);
                partner.getApiKeys().add(apiKey);
                persistenceService.save(partner);
            }

            return new ApiKeyCreationResult(apiKey, plainTextKey);
        });
    }
}
