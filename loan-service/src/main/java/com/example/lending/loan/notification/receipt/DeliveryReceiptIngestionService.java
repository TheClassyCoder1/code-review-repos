package com.example.lending.loan.notification.receipt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.zip.GZIPInputStream;

/** Delivery receipts pushed by the messaging gateway, as JSON or protobuf. */
@RestController
public class DeliveryReceiptIngestionService {

    private static final int MAX_PAYLOAD_BYTES = 1024 * 1024;
    private static final MediaType PROTOBUF = MediaType.parseMediaType("application/x-protobuf");
    private static final byte[] JSON_RESPONSE = "{}".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PROTOBUF_RESPONSE = new byte[0];

    private final DeliveryReceiptAdapter protocolAdapter;
    private final byte[] receiptToken;

    public DeliveryReceiptIngestionService(DeliveryReceiptAdapter protocolAdapter,
                                           @Value("${notifications.gateway.receipt-token}") String receiptToken) {
        this.protocolAdapter = protocolAdapter;
        this.receiptToken = receiptToken.getBytes(StandardCharsets.UTF_8);
    }

    @PostMapping("/api/v1/notifications/receipts")
    public ResponseEntity<byte[]> receive(@RequestHeader HttpHeaders headers, InputStream body) throws IOException {
        String token = headers.getFirst("X-Receipt-Token");
        if (receiptToken.length == 0 || token == null
                || !MessageDigest.isEqual(receiptToken, token.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ingestHttp(readBounded(body), headers);
    }

    public ResponseEntity<byte[]> ingestHttp(byte[] content, HttpHeaders headers) {
        HttpHeaders safeHeaders = headers == null ? HttpHeaders.EMPTY : headers;
        byte[] normalizedContent = maybeDecompress(content, safeHeaders);
        MediaType contentType = safeHeaders.getContentType();
        if (contentType != null && MediaType.APPLICATION_JSON.includes(contentType)) {
            protocolAdapter.ingest(new String(normalizedContent, StandardCharsets.UTF_8));
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(JSON_RESPONSE);
        }
        protocolAdapter.ingestBinary(normalizedContent);
        return ResponseEntity.ok().contentType(PROTOBUF).body(PROTOBUF_RESPONSE);
    }

    private static byte[] maybeDecompress(byte[] content, HttpHeaders headers) {
        if (!"gzip".equalsIgnoreCase(headers.getFirst(HttpHeaders.CONTENT_ENCODING))) {
            return content;
        }
        try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(content))) {
            return readBounded(in);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid gzip body");
        }
    }

    private static byte[] readBounded(InputStream in) throws IOException {
        byte[] bytes = in.readNBytes(MAX_PAYLOAD_BYTES + 1);
        if (bytes.length > MAX_PAYLOAD_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        }
        return bytes;
    }
}
