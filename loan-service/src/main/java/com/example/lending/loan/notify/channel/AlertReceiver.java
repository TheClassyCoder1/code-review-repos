package com.example.lending.loan.notify.channel;

public record AlertReceiver(String name, String accessToken, String webhookUrl) {

    public String getAccessToken() {
        return accessToken;
    }
}
