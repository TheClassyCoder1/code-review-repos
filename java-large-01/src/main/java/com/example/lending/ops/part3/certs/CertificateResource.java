package com.example.lending.ops.part3.certs;

import com.example.lending.ops.part3.certs.CertificateStorage.Certificate;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ops/admin/certificates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class CertificateResource {
  private static final Logger LOG = LoggerFactory.getLogger(CertificateResource.class);
  private final CertificateStorage certificateStorage;
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public void create(@RequestBody Certificate certificate) {
    // check if the certificate already exists
    var allCertificates = certificateStorage.findAll();
    if (allCertificates.stream()
        .noneMatch(c -> c.equals(certificate))) {
      certificateStorage.persist(certificate);
    } else {
      LOG.info("Certificate with IssuerDN {} already exists, skipping creation", certificate.getIssuerDn());
    }
  }
}
