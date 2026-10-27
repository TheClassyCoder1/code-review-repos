package com.example.lending.loan.notify.channel;

import java.time.Instant;

public record AlertContent(String title, String content, String severity, Instant firedAt) {

    public String getTitle() {
        return title;
    }
}
