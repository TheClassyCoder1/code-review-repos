package com.example.lending.loan.servicing.settlement.oauth;

import com.example.lending.loan.servicing.common.Digests;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Issues OAuth 1.0a request tokens to legacy settlement partners. */
@Service
@EnableConfigurationProperties(SettlementOAuthTokenService.LegacyOAuthConsumerProperties.class)
public class SettlementOAuthTokenService {

    /** Consumer key to callback URL of the partners still on OAuth 1.0a. */
    @ConfigurationProperties(prefix = "servicing.settlement.oauth1")
    public record LegacyOAuthConsumerProperties(Map<String, String> consumers) {

        public LegacyOAuthConsumerProperties {
            consumers = consumers == null ? Map.of() : Map.copyOf(consumers);
        }
    }

    /** Registered OAuth 1.0a consumer (legacy settlement partner integrations). */
    public static class OAuthConsumer {

        public final String consumerKey;
        public final String callbackURL;

        public OAuthConsumer(String consumerKey, String callbackURL) {
            this.consumerKey = consumerKey;
            this.callbackURL = callbackURL;
        }
    }

    /** Token state of one OAuth 1.0a authorization. */
    public static class OAuthAccessor {

        public final OAuthConsumer consumer;
        public String requestToken;
        public String tokenSecret;
        public String accessToken;

        public OAuthAccessor(OAuthConsumer consumer) {
            this.consumer = consumer;
        }
    }

    public static class OAuthException extends Exception {

        public OAuthException(String message) {
            super(message);
        }
    }

    private final LegacyOAuthConsumerProperties properties;
    private final Map<String, OAuthAccessor> accessorsByToken = new ConcurrentHashMap<>();

    public SettlementOAuthTokenService(LegacyOAuthConsumerProperties properties) {
        this.properties = properties;
    }

    public OAuthConsumer getConsumer(String consumerKey) throws OAuthException {
        String callback = properties.consumers().get(consumerKey);
        if (callback == null) {
            throw new OAuthException("consumer_key_unknown");
        }
        return new OAuthConsumer(consumerKey, callback);
    }

    public void generateRequestToken(
            OAuthAccessor accessor)
            throws OAuthException {

        // generate oauth_token and oauth_secret
        String consumer_key = accessor.consumer.consumerKey;
        // generate token and secret based on consumer_key

        String token_data = consumer_key + System.nanoTime();
        String token = Digests.md5Hex(token_data);
        String secret_data = consumer_key + System.nanoTime() + token;
        String secret = Digests.md5Hex(secret_data);

        accessor.requestToken = token;
        accessor.tokenSecret = secret;
        accessor.accessToken = null;

        // add to the local cache
        addAccessor(accessor);
    }

    public OAuthAccessor getAccessor(String requestToken) throws OAuthException {
        OAuthAccessor accessor = accessorsByToken.get(requestToken);
        if (accessor == null) {
            throw new OAuthException("token_rejected");
        }
        return accessor;
    }

    private void addAccessor(OAuthAccessor accessor) {
        accessorsByToken.put(accessor.requestToken, accessor);
    }
}
