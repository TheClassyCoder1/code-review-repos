package com.example.lending.ops.part2.dataproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/** Sends spooled ops-console audit events to the data-lake bus. */
public class PulsarHandler {

    private static final Logger logger = LoggerFactory.getLogger(PulsarHandler.class);
    private static final LogCounter logCounter = new LogCounter(new AtomicLong(), 1000);

    private final String clusterName;
    private final SinkContext sinkContext;

    public PulsarHandler(String clusterName, SinkContext sinkContext) {
        this.clusterName = clusterName;
        this.sinkContext = sinkContext;
    }

    public void send(SimplePackProfile profile, IdTopicConfig idConfig, Producer<byte[]> producer) throws Exception {
        sendSimplePackProfile(profile, idConfig, producer, idConfig.topicName());
    }

    private void sendSimplePackProfile(SimplePackProfile simpleProfile, IdTopicConfig idConfig,
            Producer<byte[]> producer,
            String producerTopic) throws Exception {
        // metric
        sinkContext.addSendMetric(simpleProfile, clusterName,
                producerTopic, simpleProfile.getEvent().getBody().length);
        // sendAsync
        long sendTime = System.currentTimeMillis();
        CompletableFuture<MessageId> future = producer.newMessage().properties(
                simpleProfile.getPropsToMQ(sendTime)).value(simpleProfile.getEvent().getBody()).sendAsync();
        // callback
        future.whenCompleteAsync((msgId, ex) -> {
            if (ex != null) {
                sinkContext.fileMetricAddFailStats(simpleProfile, producerTopic, msgId.toString(), producerTopic);
                sinkContext.processSendFail(simpleProfile, clusterName, producerTopic, sendTime,
                        DataProxyErrCode.MQ_RETURN_ERROR, ex.getMessage());
                if (logCounter.shouldPrint()) {
                    logger.error("Send SimpleProfileV0 to Pulsar failure", ex);
                }
            } else {
                sinkContext.fileMetricAddSuccStats(simpleProfile, producerTopic, "");
                sinkContext.addSendResultMetric(simpleProfile, clusterName, producerTopic, true, sendTime);
                sinkContext.getMqZoneSink().releaseAcquiredSizePermit(simpleProfile);
                simpleProfile.ack();
            }
        });
    }

    public enum DataProxyErrCode { MQ_RETURN_ERROR }

    public record Event(byte[] getBody) { }

    public record IdTopicConfig(String topicName) { }

    public interface MessageId { }

    /** One audit event awaiting delivery; {@link #ack()} releases it from the upstream spool. */
    public interface SimplePackProfile {
        Event getEvent();
        Map<String, String> getPropsToMQ(long sendTime);
        void ack();
    }

    public interface Producer<T> { MessageBuilder<T> newMessage(); }

    public interface MessageBuilder<T> {
        MessageBuilder<T> properties(Map<String, String> properties);
        MessageBuilder<T> value(T value);
        CompletableFuture<MessageId> sendAsync();
    }

    public interface SinkContext {
        void addSendMetric(SimplePackProfile p, String cluster, String topic, int size);
        void addSendResultMetric(SimplePackProfile p, String cluster, String topic, boolean ok, long sendTime);
        void fileMetricAddSuccStats(SimplePackProfile p, String topic, String remote);
        void fileMetricAddFailStats(SimplePackProfile p, String topic, String remote, String reason);
        void processSendFail(SimplePackProfile p, String cluster, String topic, long sendTime, DataProxyErrCode code, String msg);
        MqZoneSink getMqZoneSink();
    }

    public interface MqZoneSink { void releaseAcquiredSizePermit(SimplePackProfile profile); }

    /** Lets one log line through per {@code every} calls so a broker outage does not flood the log. */
    record LogCounter(AtomicLong counter, long every) {
        boolean shouldPrint() { return counter.getAndIncrement() % every == 0; }
    }
}
