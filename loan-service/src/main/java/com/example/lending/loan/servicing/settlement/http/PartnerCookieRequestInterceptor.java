package com.example.lending.loan.servicing.settlement.http;

import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.cookie.BasicClientCookie;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.protocol.HttpContext;

import java.io.IOException;
import java.util.Map;

/** Adds the partner reporting session cookie and fixed headers to every request. */
public abstract class PartnerCookieRequestInterceptor implements HttpRequestInterceptor {

    protected final CookieStore cookieStore;
    protected final String cookieName;
    protected final boolean isSsl;
    private final Map<String, String> additionalHeaders;
    private final Map<String, String> customCookies;

    protected PartnerCookieRequestInterceptor(CookieStore cs, String cn, boolean isSsl,
                                              Map<String, String> additionalHeaders,
                                              Map<String, String> customCookies) {
        this.cookieStore = cs == null ? new BasicCookieStore() : cs;
        this.cookieName = cn;
        this.isSsl = isSsl;
        this.additionalHeaders = additionalHeaders == null ? Map.of() : Map.copyOf(additionalHeaders);
        this.customCookies = customCookies == null ? Map.of() : Map.copyOf(customCookies);
    }

    @Override
    public void process(HttpRequest request, EntityDetails entity, HttpContext httpContext)
            throws HttpException, IOException {
        HttpClientContext context = HttpClientContext.adapt(httpContext);
        context.setCookieStore(cookieStore);
        for (Map.Entry<String, String> cookie : customCookies.entrySet()) {
            BasicClientCookie clientCookie = new BasicClientCookie(cookie.getKey(), cookie.getValue());
            clientCookie.setSecure(isSsl);
            cookieStore.addCookie(clientCookie);
        }
        additionalHeaders.forEach(request::setHeader);
        if (!hasSessionCookie()) {
            addHttpAuthHeader(request, httpContext);
        }
    }

    private boolean hasSessionCookie() {
        return cookieStore.getCookies().stream().anyMatch(cookie -> cookie.getName().equals(cookieName));
    }

    protected abstract void addHttpAuthHeader(HttpRequest request, HttpContext httpContext) throws HttpException;
}
