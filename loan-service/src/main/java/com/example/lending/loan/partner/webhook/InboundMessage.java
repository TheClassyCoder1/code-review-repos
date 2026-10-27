package com.example.lending.loan.partner.webhook;

import java.net.URI;

public record InboundMessage(String id, URI source, String type, String subject, String dataContentType, byte[] data) {
}
