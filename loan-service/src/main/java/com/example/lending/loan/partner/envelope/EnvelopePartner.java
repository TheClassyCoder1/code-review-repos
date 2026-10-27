package com.example.lending.loan.partner.envelope;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "envelope_partners", schema = "lending")
public class EnvelopePartner {

    @Id
    @Column(name = "partner_id")
    private String partnerId;

    @Column(name = "public_key")
    private String publicKey;

    @Column(name = "mac_secret")
    private String macSecret;

    public String getPartnerId() { return partnerId; }
    public String getPublicKey() { return publicKey; }
    public String getMacSecret() { return macSecret; }
}
