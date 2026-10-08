package com.example.lending.loan.auth;

import com.example.lending.loan.borrower.SessionTokenFilter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {

    private final PasswordResetService passwordResetService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(PasswordResetService passwordResetService,
                          EmailVerificationService emailVerificationService) {
        this.passwordResetService = passwordResetService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/api/v1/auth/password-reset")
    public ResponseEntity<Void> requestReset(@RequestBody ResetRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/api/v1/auth/password-reset/confirm")
    public ResponseEntity<Void> confirmReset(@RequestBody ResetConfirmation request) {
        passwordResetService.confirmReset(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/me/email-verification")
    public ResponseEntity<Void> sendVerification(@RequestAttribute(SessionTokenFilter.BORROWER_ID) Long borrowerId) {
        emailVerificationService.sendVerification(borrowerId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/api/v1/auth/email-verification/confirm")
    public ResponseEntity<Void> confirmVerification(@RequestBody VerificationConfirmation request) {
        emailVerificationService.confirm(request.token());
        return ResponseEntity.noContent().build();
    }

    public record ResetRequest(String email) {
    }

    public record ResetConfirmation(String token, String newPassword) {
    }

    public record VerificationConfirmation(String token) {
    }
}
