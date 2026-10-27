package com.example.lending.ops.mail;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.List;

/** Platform-wide settings editable from the console settings page. */
@Data
@Component
@ConfigurationProperties(prefix = "ops.platform")
public class ConfigurationStorage {

  private final EmailConfig emailConfig = new EmailConfig();

  public ConfigurationStorage get() { return this; }

  @Data
  public static class EmailConfig {
    private String relayUrl, relayUser, relayPassword;
    public boolean isEmailConfigured() { return relayUrl != null && !relayUrl.isBlank(); }
  }

  public record SpEmail(List<String> recipients, String subject, String message) { }

  /** Sends mail through the HTTP mail relay configured in the platform settings. */
  public record MailSender(ConfigurationStorage configuration) {
    public void sendEmail(SpEmail email) {
      EmailConfig cfg = configuration.getEmailConfig();
      HttpHeaders headers = new HttpHeaders();
      headers.setBasicAuth(cfg.getRelayUser(), cfg.getRelayPassword());
      new RestTemplate().postForEntity(cfg.getRelayUrl(), new HttpEntity<>(email, headers), Void.class);
    }
  }
}
