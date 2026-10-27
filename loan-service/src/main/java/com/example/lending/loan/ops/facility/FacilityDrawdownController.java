package com.example.lending.loan.ops.facility;

import com.example.lending.loan.ops.facility.FacilityLedger.CreditFacility;
import com.example.lending.loan.ops.facility.FacilityLedger.Drawdown;
import com.example.lending.loan.support.web.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/ops/facilities")
public class FacilityDrawdownController {

    private static final Logger log = LoggerFactory.getLogger(FacilityDrawdownController.class);

    private final FacilityLedger ledger;

    public FacilityDrawdownController(FacilityLedger ledger) {
        this.ledger = ledger;
    }

    @PostMapping("/drawdown")
    public ApiResponse<Map<String, Object>> deductStock(@RequestParam String orderId) {

        log.info("Processing drawdown - request ID: {}", orderId);

        Drawdown order = ledger.drawdownStore.get(orderId);
        if (order == null) {
            return ApiResponse.ofFail("Drawdown not found: " + orderId);
        }

        if ("paid".equals(order.getStatus())) {
            return ApiResponse.ofFail("Drawdown already disbursed");
        }

        if ("cancelled".equals(order.getStatus())) {
            return ApiResponse.ofFail("Drawdown was cancelled");
        }

        CreditFacility product = ledger.facilityStore.get(order.getFacilityId());
        if (product == null) {
            return ApiResponse.ofFail("Facility not found: " + order.getFacilityId());
        }

        synchronized (product) {
            if (product.getStock() < order.getQuantity()) {
                return ApiResponse.ofFail("Insufficient facility limit, available: " + product.getStock() + ", requested: " + order.getQuantity());
            }

            int newStock = product.getStock() - order.getQuantity();
            product.setStock(newStock);

            order.setStatus("paid");

            log.info("Drawdown disbursed - facility: {}, amount: {}, remaining limit: {}",
                    product.getName(), order.getQuantity(), newStock);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("orderId", orderId);
        result.put("facilityId", product.getId());
        result.put("facilityName", product.getName());
        result.put("drawnAmount", order.getQuantity());
        result.put("remainingLimit", product.getStock());
        result.put("status", order.getStatus());

        return ApiResponse.ofSuccess(result);
    }
}
