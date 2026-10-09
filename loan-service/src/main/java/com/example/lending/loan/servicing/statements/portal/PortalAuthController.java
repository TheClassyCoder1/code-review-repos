package com.example.lending.loan.servicing.statements.portal;

import com.example.lending.loan.servicing.common.ServicingResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Self-service registration and sign-in for the statement portal. */
@RestController
@RequestMapping("/servicing/portal/auth")
public class PortalAuthController {

    private final PortalUserService portalUserService;
    private final PortalLoginService portalLoginService;

    public PortalAuthController(PortalUserService portalUserService, PortalLoginService portalLoginService) {
        this.portalUserService = portalUserService;
        this.portalLoginService = portalLoginService;
    }

    @PostMapping("/register")
    public ResponseEntity<ServicingResult<Void>> register(@Valid @RequestBody RegistrationRequest request) {
        portalUserService.createUser(request.username(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(ServicingResult.ok());
    }

    @PostMapping("/login")
    public ResponseEntity<ServicingResult<Map<String, String>>> login(@Valid @RequestBody LoginRequest request) {
        return portalLoginService.login(request.username(), request.password())
                .map(token -> ResponseEntity.ok(ServicingResult.ok(Map.of("accessToken", token))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ServicingResult.failed("Invalid username or password")));
    }

    public record RegistrationRequest(@NotBlank @Size(max = 64) String username,
                                      @NotBlank @Email String email,
                                      @NotBlank @Size(min = 10, max = 128) String password) {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }
}
