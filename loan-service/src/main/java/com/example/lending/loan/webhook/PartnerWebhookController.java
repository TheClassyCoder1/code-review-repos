package com.example.lending.loan.webhook;

import com.example.lending.loan.dto.ApiResult;
import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.*;
import org.slf4j.*;
import org.springframework.http.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class PartnerWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PartnerWebhookController.class);

    private final ObjectMapper mapper;
    private final InboundEventBuffer buffer;
    private final WebhookSecrets secrets;
    private final WebhookFailureTracker failureTracker;
    private final PartnerWebhookService webhookService;

    public PartnerWebhookController(ObjectMapper mapper, InboundEventBuffer buffer, WebhookSecrets secrets,
                                    WebhookFailureTracker failureTracker, PartnerWebhookService webhookService) {
        this.mapper = mapper;
        this.buffer = buffer;
        this.secrets = secrets;
        this.failureTracker = failureTracker;
        this.webhookService = webhookService;
    }

    @GetMapping("/admin/partners/{partnerId}/webhooks")
    public List<PartnerWebhook> listWebhooks(@PathVariable String partnerId) {
        return webhookService.listWebhooks(partnerId);
    }

    @PostMapping("/admin/partners/{partnerId}/webhooks")
    public ApiResult register(@PathVariable String partnerId,
                              @RequestBody PartnerWebhookService.Registration registration) {
        return ApiResult.success(webhookService.register(partnerId, registration).getId());
    }

    @PostMapping("/admin/partners/{partnerId}/webhooks/{webhookId}/toggle")
    public ApiResult toggle(@PathVariable String partnerId, @PathVariable Long webhookId) {
        return ApiResult.success(webhookService.toggleEnabled(partnerId, webhookId).isEnabled());
    }

    @PostMapping("/admin/partners/{partnerId}/webhooks/{webhookId}/ping")
    public ApiResult sendPing(@PathVariable String partnerId, @PathVariable Long webhookId)
            throws IOException, InterruptedException {
        return ApiResult.success(webhookService.sendPing(partnerId, webhookId));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResult> onConcurrentChange(ObjectOptimisticLockingFailureException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResult.error("Webhook changed, reload and retry"));
    }

    @PostMapping("/partners/webhooks/{partnerId}")
    public void handle(@PathVariable String partnerId, HttpServletRequest request, HttpServletResponse response) {
        try {
            if (failureTracker.isLockedOut(partnerId)) {
                response.sendError(429);
                return;
            }
            byte[] body = request.getInputStream().readAllBytes();
            if (!secrets.verify(partnerId, request.getHeader(WebhookSecrets.SIGNATURE_HEADER), body)) {
                failureTracker.addSignatureFailure(partnerId);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            JsonNode root = mapper.readTree(body);
            String type = root.path("type").asText("notification");
            InboundEventBuffer.PartnerEvent event = new InboundEventBuffer.PartnerEvent(
                    "partner-" + root.path("id").asText(String.valueOf(System.nanoTime())),
                    partnerId,
                    "partner." + type,
                    body);
            if (!buffer.offer(event)) {
                response.sendError(503);
                return;
            }
            byte[] resp = "{\"result\":{\"ok\":true}}".getBytes(StandardCharsets.UTF_8);
            response.setContentType("application/json");
            response.setStatus(202);
            response.setContentLength(resp.length);
            try (OutputStream os = response.getOutputStream()) {
                os.write(resp);
            }
        } catch (Exception e) {
            log.warn("partner webhook request failed: {}", e.getClass().getSimpleName());
            try {
                response.sendError(500);
            } catch (Exception ignored) {
                // client gone
            }
        }
    }
}
