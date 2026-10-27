package com.example.lending.loan.partner.oauth;

import com.example.lending.loan.partner.account.PartnerSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.PrintWriter;

@Controller
@RequestMapping("/partner/oauth")
public class PartnerAuthorizationController {

    private final OAuthRequestTokenStore tokenStore;

    public PartnerAuthorizationController(OAuthRequestTokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @PostMapping("/authorize")
    public void authorize(HttpServletRequest request, HttpServletResponse response) throws IOException {
        OAuthAccessor accessor = tokenStore.get(request.getParameter("oauth_token"));
        if (accessor == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown request token");
            return;
        }
        accessor.setAuthorizedUserId(PartnerSession.userId(request));
        returnToConsumer(request, response, accessor);
    }

    private void returnToConsumer(HttpServletRequest request,
            HttpServletResponse response, OAuthAccessor accessor)
        throws IOException {

        // send the user back to site's callBackUrl
        String callback = request.getParameter("oauth_callback");
        if ("none".equals(callback)
            && accessor.consumer.callbackURL != null
                && accessor.consumer.callbackURL.length() > 0){
            // first check if we have something in our properties file
            callback = accessor.consumer.callbackURL;
        }

        if ( "none".equals(callback) ) {
            // no call back it must be a client
            response.setContentType("text/plain");
            try (PrintWriter out = response.getWriter()) {
                out.println("You have successfully authorized for consumer key '"
                        + accessor.consumer.consumerKey
                        + "'. Please close this browser window and click continue"
                                + " in the client.");
            }
        } else {
            // if callback is not passed in, use the callback from config
            if(callback == null || callback.length() <=0 ) {
                callback = accessor.consumer.callbackURL;
            }
            String token = accessor.requestToken;
            if (token != null && callback != null) {
                callback = UriComponentsBuilder.fromUriString(callback)
                        .queryParam("oauth_token", token).build().toUriString();
            }

            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            response.setHeader("Location", callback);
        }
    }
}
