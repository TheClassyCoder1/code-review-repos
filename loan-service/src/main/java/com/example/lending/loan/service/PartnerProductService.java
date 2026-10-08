package com.example.lending.loan.service;

import com.example.lending.loan.dto.PartnerProductDefinition;
import com.example.lending.loan.entity.PartnerProduct;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartnerProductService {

    private final EntityManager entityManager;

    public PartnerProductService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public boolean productDefinitionExists(String identifier) {
        return entityManager.find(PartnerProduct.class, identifier) != null;
    }

    @Transactional
    public void create(PartnerProductDefinition definition) {
        PartnerProduct product = new PartnerProduct();
        product.setIdentifier(definition.identifier());
        product.setName(definition.name());
        product.setRate(definition.rate());
        entityManager.persist(product);
        entityManager.flush();
    }
}
