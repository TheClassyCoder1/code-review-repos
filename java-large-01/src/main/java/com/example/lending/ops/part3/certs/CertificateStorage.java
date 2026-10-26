package com.example.lending.ops.part3.certs;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;

/** Trusted partner TLS certificates. (issuer_dn, serial_number) is the table's primary key and inserts ignore conflicts, so persisting the same certificate twice is a no-op even when two requests race. */
@Repository
@RequiredArgsConstructor
public class CertificateStorage {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    public List<Certificate> findAll() {
        return jdbcTemplate.query("SELECT issuer_dn, serial_number, pem FROM partner_certificate", (rs, i) -> new Certificate(rs.getString(1), rs.getString(2), rs.getString(3)));
    }
    public void persist(Certificate certificate) {
        jdbcTemplate.update("INSERT INTO partner_certificate (issuer_dn, serial_number, pem) VALUES (:issuer, :serial, :pem) "
                + "ON CONFLICT (issuer_dn, serial_number) DO NOTHING",
                Map.of("issuer", certificate.issuerDn(), "serial", certificate.serialNumber(), "pem", certificate.pem()));
    }
    public record Certificate(String issuerDn, String serialNumber, String pem) { public String getIssuerDn() { return issuerDn; } }
}
