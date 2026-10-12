package com.example.lending.loan.servicing.settlement.broker;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/** Connection settings parsed from an {@code amqp://} or {@code amqps://} URI. */
public class BrokerConnectionFactory {

    private String host;
    private int port;
    private String virtualHost = "/";
    private String username;
    private String password;
    private SSLContext sslContext;

    public void setUri(String uriString) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        URI uri = new URI(uriString);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        switch (scheme) {
            case "amqp" -> port = uri.getPort() == -1 ? 15672 : uri.getPort();
            case "amqps" -> {
                port = uri.getPort() == -1 ? 15671 : uri.getPort();
                sslContext = SSLContext.getInstance("TLSv1.3");
                sslContext.init(null, null, null);
            }
            default -> throw new URISyntaxException(uriString, "Wrong scheme in broker URI");
        }
        if (uri.getHost() == null) {
            throw new URISyntaxException(uriString, "Missing host in broker URI");
        }
        host = uri.getHost();
        String userInfo = uri.getRawUserInfo();
        if (userInfo != null) {
            String[] credentials = userInfo.split(":", 2);
            username = URLDecoder.decode(credentials[0], StandardCharsets.UTF_8);
            password = credentials.length > 1 ? URLDecoder.decode(credentials[1], StandardCharsets.UTF_8) : "";
        }
        String path = uri.getRawPath();
        if (path != null && path.length() > 1) {
            virtualHost = URLDecoder.decode(path.substring(1), StandardCharsets.UTF_8);
        }
    }

    public String getHost() { return host; }
    public int getPort() { return port; }

    public String getVirtualHost() { return virtualHost; }
    public String getUsername() { return username; }

    public String getPassword() { return password; }
    public SSLContext getSslContext() { return sslContext; }

    public boolean isSsl() { return sslContext != null; }
}
