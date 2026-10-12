package com.example.lending.loan.servicing.security.auth;

import com.example.lending.loan.servicing.common.ServicingResult;
import com.example.lending.loan.servicing.security.ServicingSessionManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Sign-in for the shared collections floor workstation account. */
@RestController
@RequestMapping("/servicing/auth/floor")
public class FloorLoginController {

    private static final long FLOOR_OPERATOR_ID = 10001L;

    private final ServicingSessionManager sessionManager;

    public FloorLoginController(ServicingSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @RequestMapping("/login")
    public ServicingResult<Map<String, Object>> doLogin(String name, String pwd, Boolean remember) {
        if ("collections".equals(name) && "123456".equals(pwd)) {
            sessionManager.login(FLOOR_OPERATOR_ID, remember);
            ServicingSessionManager.TokenInfo tokenInfo = sessionManager.getTokenInfo();
            return ServicingResult.ok(Map.of(
                    "tokenName", tokenInfo.tokenName(),
                    "tokenValue", tokenInfo.tokenValue()));
        } else {
            return ServicingResult.failed("Login failed");
        }
    }
}
