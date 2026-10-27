package com.example.lending.ops.part3.webhook;

import com.example.lending.ops.part3.webhook.WebhookModel.*;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Registry of chat-robot senders, one per business code. */
public final class OaFactory {
    public interface OaSender { void senderAsync(Request request, MessageType type, Consumer<Object> callback); }
    private static final Map<String, OaSender> SENDERS = new ConcurrentHashMap<>();
    private static final RestTemplate REST =
            new RestTemplateBuilder().setConnectTimeout(Duration.ofSeconds(3)).setReadTimeout(Duration.ofSeconds(5)).build();
    private static final Map<Class<?>, String> ENDPOINTS = Map.of(DingTalkConfig.class, "https://oapi.dingtalk.com/robot/send?access_token=",
            ByteTalkConfig.class, "https://open.feishu.cn/open-apis/bot/v2/hook/", WeTalkConfig.class, "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=");
    public static OaSender getSmsOaBlend(String configId) { return SENDERS.get(configId); }
    public static void createAndRegisterOaSender(OaConfig config) {
        String url = ENDPOINTS.get(config.getClass()) + config.getTokenId();
        SENDERS.put(config.getConfigId(), (request, type, callback) -> CompletableFuture
                .supplyAsync(() -> REST.postForObject(url, Map.of("msgtype", type.name(), "content", String.valueOf(request.getContent())), String.class))
                .whenComplete((body, error) -> callback.accept(error == null ? body : error.getMessage())));
    }
}
