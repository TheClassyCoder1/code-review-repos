package com.example.lending.loan.notification.receipt;

import com.example.lending.loan.notification.NoticeStore;
import com.example.lending.loan.notification.receipt.proto.DeliveryReceipt;
import com.example.lending.loan.notification.receipt.proto.DeliveryReceiptBatch;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

/** Applies gateway delivery receipts (JSON or protobuf) to the matching notices. */
@Component
public class DeliveryReceiptAdapter {

    private static final Set<String> FINAL_STATUSES = Set.of("DELIVERED", "FAILED", "BOUNCED");

    private final NoticeStore noticeStore;
    private final ObjectMapper objectMapper;

    public DeliveryReceiptAdapter(NoticeStore noticeStore, ObjectMapper objectMapper) {
        this.noticeStore = noticeStore;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void ingest(String json) {
        try {
            DeliveryReceiptBatch.Builder batch = DeliveryReceiptBatch.newBuilder();
            for (JsonNode node : objectMapper.readTree(json).path("receipts")) {
                batch.addReceipts(DeliveryReceipt.newBuilder()
                        .setMessageId(node.path("message_id").asText())
                        .setStatus(node.path("status").asText())
                        .setDeliveredAtMillis(node.path("delivered_at_millis").asLong()));
            }
            apply(batch.build());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed receipt batch");
        }
    }

    @Transactional
    public void ingestBinary(byte[] content) {
        try {
            apply(DeliveryReceiptBatch.parseFrom(content));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed receipt batch");
        }
    }

    private void apply(DeliveryReceiptBatch batch) {
        for (DeliveryReceipt receipt : batch.getReceiptsList()) {
            if (!receipt.getMessageId().isEmpty() && FINAL_STATUSES.contains(receipt.getStatus())) {
                noticeStore.applyReceipt(receipt.getMessageId(), receipt.getStatus(),
                        Instant.ofEpochMilli(receipt.getDeliveredAtMillis()));
            }
        }
    }
}
