package com.example.lending.ops.part4.pricing;

import com.example.lending.ops.part4.engine.Debug;
import com.example.lending.ops.part4.engine.DispatchContext;
import com.example.lending.ops.part4.engine.GenericDispatcher;
import com.example.lending.ops.part4.engine.GenericEntityException;
import com.example.lending.ops.part4.engine.GenericServiceException;
import com.example.lending.ops.part4.engine.GenericValue;
import com.example.lending.ops.part4.engine.LocalDispatcher;
import com.example.lending.ops.part4.engine.ServiceUtil;
import com.example.lending.ops.part4.engine.UtilMisc;
import com.example.lending.ops.part4.engine.UtilProperties;
import com.example.lending.ops.part4.engine.UtilValidate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;

/** Fee quotes for lending add-ons; stores flagged {@code showPricesWithVatTax} show every amount tax-inclusive. */
public final class FeeQuotePriceServices {

    private static final String MODULE = FeeQuotePriceServices.class.getName();
    private static final String RESOURCE = "i18n/pricing";
    private static final BigDecimal ONE_BASE = BigDecimal.ONE;
    private static final BigDecimal PERCENT_SCALE = new BigDecimal("100.000");
    private static final int TAX_SCALE = 3;
    private static final int TAX_FINAL_SCALE = 2;
    private static final RoundingMode TAX_ROUNDING = RoundingMode.HALF_UP;

    private FeeQuotePriceServices() { }

    public static void register(GenericDispatcher dispatcher) {
        dispatcher.register("calculateFeeQuote", FeeQuotePriceServices::calculateFeeQuote);
    }

    public static Map<String, Object> calculateFeeQuote(DispatchContext dctx, Map<String, ? extends Object> context) {
        String productId = (String) context.get("productId");
        Locale locale = context.get("locale") == null ? Locale.ROOT : (Locale) context.get("locale");
        Map<String, Object> result = ServiceUtil.returnSuccess();
        GenericValue productStore;
        try {
            productStore = dctx.getDelegator().findOne("ProductStore", (String) context.get("productStoreId"));
            for (String type : new String[] {"listPrice", "defaultPrice", "promoPrice", "competitivePrice", "specialPromoPrice"}) {
                GenericValue row = dctx.getDelegator().findOne("ProductPrice", productId + ":" + type);
                result.put(type, row);
            }
        } catch (GenericEntityException e) {
            Debug.logError(e, "Could not load prices for product " + productId, MODULE);
            return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE, "ProductPriceLookupProblem", locale));
        }
        for (String type : new String[] {"listPrice", "defaultPrice", "promoPrice"}) {
            result.put(type, result.get(type) == null ? null : ((GenericValue) result.get(type)).getBigDecimal("price"));
        }
        result.put("price", result.get("defaultPrice") != null ? result.get("defaultPrice") : result.get("listPrice"));
        Map<String, Object> errorResult = addGeneralResults(result, (GenericValue) result.get("competitivePrice"),
                (GenericValue) result.get("specialPromoPrice"), productStore, "Y", "EUR", productId,
                (BigDecimal) context.get("quantity"), (String) context.get("partyId"), dctx.getDispatcher(), locale);
        return errorResult != null ? errorResult : result;
    }

    public static Map<String, Object> addGeneralResults(Map<String, Object> result, GenericValue competitivePriceValue,
                                                        GenericValue specialPromoPriceValue, GenericValue productStore, String checkIncludeVat,
                                                        String currencyUomId, String productId,
                                                        BigDecimal quantity, String partyId, LocalDispatcher dispatcher, Locale locale) {
        result.put("competitivePrice", competitivePriceValue != null ? competitivePriceValue.getBigDecimal("price") : null);
        result.put("specialPromoPrice", specialPromoPriceValue != null ? specialPromoPriceValue.getBigDecimal("price") : null);
        result.put("currencyUsed", currencyUomId);

        // okay, now we have the calculated price, see if we should add in tax and if so do it
        if ("Y".equals(checkIncludeVat) && productStore != null && "Y".equals(productStore.getString("showPricesWithVatTax"))) {
            Map<String, Object> calcTaxForDisplayContext = UtilMisc.toMap("productStoreId", productStore.get("productStoreId"),
                    "productId", productId, "quantity", quantity,
                    "basePrice", (BigDecimal) result.get("price"));
            if (UtilValidate.isNotEmpty(partyId)) {
                calcTaxForDisplayContext.put("billToPartyId", partyId);
            }

            try {
                Map<String, Object> calcTaxForDisplayResult = dispatcher.runSync("calcTaxForDisplay", calcTaxForDisplayContext);
                if (ServiceUtil.isError(calcTaxForDisplayResult)) {
                    return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                            "ProductPriceCannotCalculateVatTax", locale), null, null, calcTaxForDisplayResult);
                }
                // taxTotal, taxPercentage, priceWithTax
                result.put("price", calcTaxForDisplayResult.get("priceWithTax"));

                // based on the taxPercentage calculate the other amounts, including: listPrice, defaultPrice, averageCost,
                // promoPrice, competitivePrice
                BigDecimal taxPercentage = (BigDecimal) calcTaxForDisplayResult.get("taxPercentage");
                BigDecimal taxMultiplier = ONE_BASE.add(taxPercentage.divide(PERCENT_SCALE, TAX_SCALE, TAX_ROUNDING));
                if (result.get("listPrice") != null) {
                    result.put("listPrice", ((BigDecimal) result.get("listPrice")).multiply(taxMultiplier).setScale(TAX_FINAL_SCALE, TAX_ROUNDING));
                }
                if (result.get("defaultPrice") != null) {
                    result.put("defaultPrice", ((BigDecimal) result.get("defaultPrice")).multiply(taxMultiplier)
                            .setScale(TAX_FINAL_SCALE, TAX_ROUNDING));
                }
                if (result.get("averageCost") != null) {
                    result.put("averageCost", ((BigDecimal) result.get("averageCost")).multiply(taxMultiplier)
                            .setScale(TAX_FINAL_SCALE, TAX_ROUNDING));
                }
                if (result.get("promoPrice") != null) {
                    result.put("promoPrice", ((BigDecimal) result.get("promoPrice")).multiply(taxMultiplier).setScale(TAX_FINAL_SCALE, TAX_ROUNDING));
                }
                if (result.get("competitivePrice") != null) {
                    result.put("competitivePrice", ((BigDecimal) result.get("competitivePrice")).multiply(taxMultiplier)
                            .setScale(TAX_FINAL_SCALE, TAX_ROUNDING));
                }
            } catch (GenericServiceException e) {
                Debug.logError(e, "Error calculating VAT tax (with calcTaxForDisplay service): " + e.toString(), MODULE);
                return ServiceUtil.returnError(UtilProperties.getMessage(RESOURCE,
                        "ProductPriceCannotCalculateVatTax", locale));
            }
        }

        return null;
    }
}
