package com.example.lending.ops.mail;

import com.example.lending.ops.mail.ConfigurationStorage.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Lets ops staff send ad-hoc notices (e.g. branch closures) through the platform mail relay. */
@RestController
@RequestMapping("/api/v2/mail")
@RequiredArgsConstructor
public class EmailResource extends AbstractAuthGuardedRestResource {
  private final ConfigurationStorage configurationStorage;

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("this.hasWriteAuthority()")
  public ResponseEntity<?> sendEmail(@RequestBody SpEmail email) {
    var configuration = configurationStorage.get();
    if (configuration.getEmailConfig().isEmailConfigured()) {
      try {
        new MailSender(configuration).sendEmail(email);
        return ok();
      } catch (Exception e) {
        return badRequest(e);
      }
    } else {
      return serverError(
          "Could not send email - no valid mail configuration provided in StreamPipes (go to settings -> mail)");
    }
  }
}
