package com.example.lending.ops.part4.collateral;

import com.example.lending.ops.part4.engine.Delegator;
import com.example.lending.ops.part4.engine.GenericDispatcher;
import com.example.lending.ops.part4.pricing.FeeQuotePriceServices;
import com.example.lending.ops.part4.pricing.TaxDisplayServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Builds the back-office service engine used by the collateral desk and the fee-quote screen. */
@Configuration
public class BackOfficeEngineConfig {

    @Bean
    public Delegator backOfficeDelegator() {
        Delegator delegator = new Delegator();
        CollateralServiceDefinitions.defineEntities(delegator);
        delegator.defineEntity("ProductStore", "productStoreId");
        delegator.defineEntity("ProductPrice", "productPriceId");
        delegator.defineEntity("StoreTaxRate", "productStoreId");
        return delegator;
    }

    @Bean
    public GenericDispatcher backOfficeDispatcher(Delegator backOfficeDelegator) {
        GenericDispatcher dispatcher = new GenericDispatcher(backOfficeDelegator);
        CollateralServiceDefinitions.register(dispatcher);
        TaxDisplayServices.register(dispatcher);
        FeeQuotePriceServices.register(dispatcher);
        return dispatcher;
    }
}
