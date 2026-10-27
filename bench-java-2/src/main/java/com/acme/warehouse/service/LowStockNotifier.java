package com.acme.warehouse.service;

import com.acme.warehouse.model.StockItem;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class LowStockNotifier {

    private static final Logger log = LoggerFactory.getLogger(LowStockNotifier.class);

    private final RestTemplate purchasingRestTemplate;

    public LowStockNotifier(RestTemplate purchasingRestTemplate) {
        this.purchasingRestTemplate = purchasingRestTemplate;
    }

    @Async
    public void notifyBuyer(StockItem item) {
        Map<String, Object> body = Map.of(
                "sku", item.sku(),
                "available", item.quantity() - item.reserved(),
                "reorderPoint", item.reorderPoint());
        purchasingRestTemplate.postForEntity("/low-stock", body, Void.class);
        log.info("Low-stock alert sent for {}", item.sku());
    }
}
