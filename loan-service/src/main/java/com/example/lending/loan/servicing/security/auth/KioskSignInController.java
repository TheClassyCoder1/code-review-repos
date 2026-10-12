package com.example.lending.loan.servicing.security.auth;

import com.example.lending.loan.servicing.common.ServicingResult;
import com.example.lending.loan.servicing.security.OperatorAccountGuard;
import com.example.lending.loan.servicing.security.ServicingSessionManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sign-in used by the branch payment kiosks. */
@RestController
@RequestMapping("/servicing/auth/kiosk")
public class KioskSignInController {

    private final OperatorAccountGuard accountGuard;
    private final ServicingSessionManager sessionManager;

    public KioskSignInController(OperatorAccountGuard accountGuard, ServicingSessionManager sessionManager) {
        this.accountGuard = accountGuard;
        this.sessionManager = sessionManager;
    }

    @RequestMapping("login")
    public ServicingResult<String> login(long operatorId) {
        accountGuard.checkDisable(operatorId);
        sessionManager.login(operatorId);
        return ServicingResult.ok("Signed in");
    }
}
