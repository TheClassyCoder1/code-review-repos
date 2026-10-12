package com.example.lending.loan.servicing.settlement.partners;

import com.example.lending.loan.servicing.common.ServicingException;
import com.example.lending.loan.servicing.settlement.crypto.PartnerSecretCodec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Settlement partner registry. */
@Service
public class SettlementPartnerService {

    private final SettlementPartnerRepository repository;
    private final PartnerSecretCodec secretCodec;

    public SettlementPartnerService(SettlementPartnerRepository repository, PartnerSecretCodec secretCodec) {
        this.repository = repository;
        this.secretCodec = secretCodec;
    }

    @Transactional(readOnly = true)
    public SettlementPartner get(String id) {
        return repository.findById(id)
                .orElseThrow(() -> ServicingException.notFound("Settlement partner " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<SettlementPartner> listForTenant(String tenantId) {
        return repository.findByTenantIdOrderByName(tenantId);
    }


    public void decoderSecret(SettlementPartner partner) {
        partner.setClientSecret(secretCodec.decode(partner.getClientSecret()));
    }
}
