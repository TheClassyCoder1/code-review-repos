package com.example.lending.ops.part4.pricing;

import com.example.lending.ops.part4.engine.GenericDispatcher;
import com.example.lending.ops.part4.engine.GenericValue;
import com.example.lending.ops.part4.engine.ServiceUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Display-only tax for fee quotes. A success result always carries {@code taxPercentage} and
 * {@code priceWithTax}; a store with no configured rate gets {@link BigDecimal#ZERO}, never an absent percentage.
 * A missing base price is answered with an error result.
 */
public final class TaxDisplayServices {

    private TaxDisplayServices() { }

    public static void register(GenericDispatcher dispatcher) {
        dispatcher.register("calcTaxForDisplay", (dctx, context) -> {
            BigDecimal basePrice = (BigDecimal) context.get("basePrice");
            if (basePrice == null) {
                return ServiceUtil.returnError("A base price is required to calculate display tax");
            }
            BigDecimal taxPercentage = BigDecimal.ZERO;
            try {
                GenericValue rate = dctx.getDelegator().findOne("StoreTaxRate", String.valueOf(context.get("productStoreId")));
                if (rate != null && rate.getBigDecimal("taxPercentage") != null) {
                    taxPercentage = rate.getBigDecimal("taxPercentage");
                }
            } catch (Exception e) {
                return ServiceUtil.returnError("Could not read tax rate for store " + context.get("productStoreId"));
            }
            Map<String, Object> result = ServiceUtil.returnSuccess();
            result.put("taxPercentage", taxPercentage);
            result.put("priceWithTax", basePrice.multiply(BigDecimal.ONE.add(taxPercentage.movePointLeft(2))).setScale(2, RoundingMode.HALF_UP));
            return result;
        });
    }
}
