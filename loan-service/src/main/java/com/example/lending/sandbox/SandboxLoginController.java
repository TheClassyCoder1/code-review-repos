package com.example.lending.sandbox;

import com.example.lending.loan.servicing.common.ServicingResult;
import com.example.lending.loan.servicing.security.ServicingSessionManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Signs a developer in as the seeded sandbox operator. */
@RestController
@RequestMapping("/sandbox/auth")
public class SandboxLoginController {

    private final ServicingSessionManager sessionManager;

    public SandboxLoginController(ServicingSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @RequestMapping("doLogin")
    public ServicingResult<String> doLogin(String name, String pwd) {

        if ("demo".equals(name) && "123456".equals(pwd)) {

            sessionManager.login(10001);

            return ServicingResult.ok("Signed in");
        }

        return ServicingResult.failed("Login failed");
    }
}
