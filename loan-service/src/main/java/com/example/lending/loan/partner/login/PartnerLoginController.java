package com.example.lending.loan.partner.login;

import com.example.lending.loan.partner.account.PartnerSession;
import com.example.lending.loan.partner.account.PartnerUser;
import com.example.lending.loan.partner.account.PartnerUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/partner/login")
public class PartnerLoginController {

    private static final String SMS_TOPIC = "notification.sms";

    private final PartnerOtpService otpService;
    private final PartnerUserRepository partnerUserRepository;
    private final KafkaTemplate<String, String> notificationTemplate;

    public PartnerLoginController(PartnerOtpService otpService,
                                  PartnerUserRepository partnerUserRepository,
                                  @Qualifier("brokerATemplate") KafkaTemplate<String, String> notificationTemplate) {
        this.otpService = otpService;
        this.partnerUserRepository = partnerUserRepository;
        this.notificationTemplate = notificationTemplate;
    }

    @PostMapping("/code")
    public ResponseEntity<Void> requestCode(@RequestParam String telephone) {
        Optional<PartnerUser> user = partnerUserRepository.findByTelephone(telephone);
        if (user.isPresent()) {
            String code = otpService.generateAuthCode(telephone);
            notificationTemplate.send(SMS_TOPIC, String.valueOf(user.get().getId()),
                    "Your lending partner portal sign-in code is " + code);
        }
        return ResponseEntity.accepted().build();
    }

    @PostMapping
    public ResponseEntity<Void> login(HttpServletRequest request, @RequestParam String telephone, @RequestParam String code) {
        if (!otpService.verifyAuthCode(telephone, code)) {
            return ResponseEntity.status(401).build();
        }
        PartnerUser user = partnerUserRepository.findByTelephone(telephone).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        PartnerSession.userLogin(request, user);
        return ResponseEntity.noContent().build();
    }
}
