package com.example.lending.loan.notify.template;

public record NotificationTemplateSaveRequest(String eventType, String channel, String body) {
}
