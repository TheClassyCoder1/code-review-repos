package com.example.lending.loan.notify.channel;

public record NotificationEvent(String id, String type, byte[] payload) {

    public byte[] getData() {
        return payload;
    }
}
