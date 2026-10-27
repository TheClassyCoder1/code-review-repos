package com.example.lending.loan.partner.oauth;

public class OAuthAccessor {

    public static class Consumer {
        public final String consumerKey;
        public final String callbackURL;

        public Consumer(String consumerKey, String callbackURL) {
            this.consumerKey = consumerKey;
            this.callbackURL = callbackURL;
        }
    }

    public final Consumer consumer;
    public final String requestToken;
    private Long authorizedUserId;

    public OAuthAccessor(Consumer consumer, String requestToken) {
        this.consumer = consumer;
        this.requestToken = requestToken;
    }

    public Long getAuthorizedUserId() { return authorizedUserId; }
    public void setAuthorizedUserId(Long authorizedUserId) { this.authorizedUserId = authorizedUserId; }
}
