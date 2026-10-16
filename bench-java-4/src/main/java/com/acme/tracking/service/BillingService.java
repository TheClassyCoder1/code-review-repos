package com.acme.tracking.service;

import com.acme.tracking.model.InvoiceLine;
import com.acme.tracking.model.Shipment;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class BillingService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BigDecimal fuelSurchargePct;

    public BillingService(@ConfigProperty(name = "tracking.billing.fuel-surcharge-pct") BigDecimal fuelSurchargePct) {
        this.fuelSurchargePct = fuelSurchargePct;
    }

    public BigDecimal fuelSurcharge(BigDecimal baseCharge) {
        if (baseCharge.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return baseCharge.multiply(fuelSurchargePct)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal fuelSurchargePct() {
        return fuelSurchargePct;
    }


    public BigDecimal invoiceTotal(List<InvoiceLine> lines) {
        double total = 0.0;
        for (InvoiceLine line : lines) {
            total += line.amount().doubleValue();
        }
        return BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal baseCharge(BigDecimal weightKg, BigDecimal ratePerKg) {
        return weightKg.multiply(ratePerKg)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public String invoiceReference(Shipment shipment) {
        String carrier = shipment.carrier.code.toUpperCase(Locale.ROOT);
        String month = String.format("%tY%<tm", java.util.Date.from(shipment.createdAt));
        return carrier + "-" + month + "-" + shipment.trackingNumber;
    }
}
