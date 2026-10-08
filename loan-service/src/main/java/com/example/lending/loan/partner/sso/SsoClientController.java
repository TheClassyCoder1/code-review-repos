package com.example.lending.loan.partner.sso;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class SsoClientController {

    private final SsoRequestClient ssoRequestClient;

    public SsoClientController(SsoRequestClient ssoRequestClient) {
        this.ssoRequestClient = ssoRequestClient;
    }

    @RequestMapping("/partner/sso/myInfo")
    public Object myInfo(HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return "Not signed in";
        }

        Object loginId = session.getAttribute("userId");
        Map<String, String> params = new LinkedHashMap<>();
        params.put("msgType", "userinfo");
        params.put("client", ssoRequestClient.clientId());
        params.put("loginId", String.valueOf(loginId));
        ssoRequestClient.addSignParams(params);
        String pushUrl = ssoRequestClient.buildUrl(ssoRequestClient.pushUrl(), params);
        Map<String, Object> result = ssoRequestClient.request(pushUrl);

        return result;
    }
}
