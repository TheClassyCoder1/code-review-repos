package com.example.lending.loan.servicing.settlement.partners;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

/** API key issued to a settlement partner. Only a hash and a masked form are stored. */
@Embeddable
public class PartnerApiKey {

    public enum ApiKeyType { PUBLIC, PRIVATE }

    private static final SecureRandom RANDOM = new SecureRandom();
    @Column(name = "item_id", nullable = false)
    private String itemId;
    @Column(name = "key_hash", nullable = false)
    private String keyHash;
    @Column(name = "masked_key", nullable = false)
    private String maskedKey;
    @Enumerated(EnumType.STRING)
    @Column(name = "key_type", nullable = false)
    private ApiKeyType keyType;
    @Column(name = "creation_date", nullable = false)
    private Date creationDate;
    @Column(name = "expiration_date")
    private Date expirationDate;

    public static String generatePlainTextKey() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return "stl_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String maskPlainTextKey(String plainTextKey) {
        return plainTextKey.substring(0, 8) + "..." + plainTextKey.substring(plainTextKey.length() - 4);
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    @JsonIgnore
    public String getKeyHash() { return keyHash; }
    public void setKeyHash(String keyHash) { this.keyHash = keyHash; }
    public String getMaskedKey() { return maskedKey; }
    public void setMaskedKey(String maskedKey) { this.maskedKey = maskedKey; }
    public ApiKeyType getKeyType() { return keyType; }
    public void setKeyType(ApiKeyType keyType) { this.keyType = keyType; }
    public Date getCreationDate() { return creationDate; }
    public void setCreationDate(Date creationDate) { this.creationDate = creationDate; }
    public Date getExpirationDate() { return expirationDate; }
    public void setExpirationDate(Date expirationDate) { this.expirationDate = expirationDate; }
}
