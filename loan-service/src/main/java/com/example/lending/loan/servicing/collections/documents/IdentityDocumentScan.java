package com.example.lending.loan.servicing.collections.documents;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Scanned page of an identification card. */
@Entity
@Table(name = "servicing_identification_scans", schema = "lending",
        uniqueConstraints = @UniqueConstraint(name = "uk_identification_scan",
                columnNames = {"card_number", "identifier"}))
public class IdentityDocumentScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "card_number", nullable = false)
    private String cardNumber;
    @Column(name = "identifier", nullable = false)
    private String identifier;
    @Column(name = "description", length = 4096)
    private String description;
    @Column(name = "content_type", nullable = false)
    private String contentType;
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "image", nullable = false)
    private byte[] image;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public byte[] getImage() { return image; }
    public void setImage(byte[] image) { this.image = image; }
}
