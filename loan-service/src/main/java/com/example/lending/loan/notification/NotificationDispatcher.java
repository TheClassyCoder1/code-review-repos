package com.example.lending.loan.notification;

import com.example.lending.loan.scheduling.ScheduledTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/** Sends pending borrower notices through the messaging gateway. */
@Service
public class NotificationDispatcher implements ScheduledTask {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final NotificationMetadata DEFAULT_METADATA =
            NotificationMetadata.newBuilder().setType("notice").putHeader("source", "loan-service").build();

    private final NoticeStore noticeStore;
    private final NoticeTemplateService templateService;
    private final PortalLinkBuilder portalLinkBuilder;
    private final PortalLinkBuilder.PortalApp portalApp;
    private final WebClient gateway;

    public NotificationDispatcher(NoticeStore noticeStore, NoticeTemplateService templateService,
                                  PortalLinkBuilder portalLinkBuilder, WebClient.Builder builder,
                                  @Value("${notifications.gateway.url}") String gatewayUrl,
                                  @Value("${notifications.portal.client-code}") String portalClientCode,
                                  @Value("${notifications.portal.shared-key}") String portalSharedKey,
                                  @Value("${notifications.portal.login-url}") String portalLoginUrl) {
        this.noticeStore = noticeStore;
        this.templateService = templateService;
        this.portalLinkBuilder = portalLinkBuilder;
        this.portalApp = new PortalLinkBuilder.PortalApp(portalClientCode, portalSharedKey, portalLoginUrl, null);
        this.gateway = builder.clone().baseUrl(gatewayUrl).build();
    }

    @Override
    public String name() {
        return "notice-dispatch";
    }

    @Override
    public void run() {
        int maxAttempts;
        try {
            maxAttempts = RetryOptionsUtils.getDefaultOptions().maxAttempts();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        for (Notice notice : noticeStore.findPending()) {
            try {
                String link = portalLinkBuilder.authorize(portalApp, "borrower-" + notice.userId());
                String body = templateService.render(notice.templateKey(),
                        Map.of("loanId", String.valueOf(notice.loanId()), "portalLink", link));
                NotificationMetadata metadata = NotificationMetadata.newBuilder().mergeFrom(DEFAULT_METADATA)
                        .setType(notice.templateKey()).putHeader("notice-id", String.valueOf(notice.id())).build();
                Map<?, ?> response = gateway.post().uri("/v1/messages")
                        .bodyValue(Map.of("user_id", notice.userId(), "channel", notice.channel(), "body", body,
                                "type", metadata.getType(), "headers", metadata.getHeaders()))
                        .retrieve().bodyToMono(Map.class).block(TIMEOUT);
                if (response == null || response.get("message_id") == null) {
                    throw new IllegalStateException("Gateway returned no message id");
                }
                noticeStore.markSent(notice.id(), String.valueOf(response.get("message_id")));
            } catch (Exception e) {
                int attempts = notice.attempts() + 1;
                noticeStore.recordFailure(notice.id(), attempts, attempts >= maxAttempts);
                log.warn("Notice {} delivery attempt {} failed: {}", notice.id(), attempts, e.getClass().getSimpleName());
            }
        }
    }
}
