package com.example.lending.loan.ops;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

@RestController
public class OpsSetupController {

    private final OpsAccess opsAccess;

    public OpsSetupController(OpsAccess opsAccess) {
        this.opsAccess = opsAccess;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startSetupIfNeeded() {
        if (opsAccess.noAdminExists()) {
            OpsBootstrapSecurity.start();
        }
    }

    @PostMapping("/internal/ops/setup")
    public ResponseEntity<Void> setup(@RequestBody Map<String, String> body, HttpServletRequest request) {
        if (!isLoopback(request.getRemoteAddr()) || !OpsBootstrapSecurity.redeem(body.get("token"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        opsAccess.createAdmin(body.get("username"), body.get("display_name"));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private static boolean isLoopback(String address) {
        try {
            return InetAddress.getByName(address).isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
