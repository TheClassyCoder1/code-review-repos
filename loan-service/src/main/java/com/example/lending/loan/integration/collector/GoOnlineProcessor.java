package com.example.lending.loan.integration.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GoOnlineProcessor {

    private static final Logger log = LoggerFactory.getLogger(GoOnlineProcessor.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int SUCCESS_CODE = 0;

    public interface CollectorDispatch {
        void goOnline();
    }

    private final CollectorDispatch timerDispatch;

    public GoOnlineProcessor(CollectorDispatch timerDispatch) {
        this.timerDispatch = timerDispatch;
    }

    public ClusterMessage handle(ClusterMessage message) {
        if (message.getMsg().isEmpty()) {
            log.warn("The message that server response to collector is empty, please upgrade server");
        } else {
            JsonNode serverInfo = readServerInfo(message.getMsg());
            if (serverInfo == null || !serverInfo.hasNonNull("aesSecret")) {
                log.warn("The message that server response to collector has not secret empty, please check");
            } else {
                CollectorCrypto.setDefaultSecretKey(serverInfo.get("aesSecret").asText());
            }
        }
        if (ClusterMessage.Direction.REQUEST.equals(message.getDirection())) {
            timerDispatch.goOnline();
        }
        log.info("receive online message and handle success");
        return new ClusterMessage(message.getIdentity(), ClusterMessage.Direction.RESPONSE, String.valueOf(SUCCESS_CODE));
    }

    private static JsonNode readServerInfo(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (IOException e) {
            return null;
        }
    }
}
