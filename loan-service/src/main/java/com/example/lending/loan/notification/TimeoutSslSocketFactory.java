package com.example.lending.loan.notification;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.*;

/** TLS socket factory whose sockets always connect and read with bounded timeouts. */
public final class TimeoutSslSocketFactory extends SSLSocketFactory {

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 10_000;
    private static final SSLSocketFactory DELEGATE = (SSLSocketFactory) SSLSocketFactory.getDefault();
    private static final TimeoutSslSocketFactory DEFAULT = new TimeoutSslSocketFactory();

    public static SSLSocketFactory getDefault() {
        return DEFAULT;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        Socket raw = new Socket();
        try {
            raw.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            raw.setSoTimeout(READ_TIMEOUT_MS);
            return DELEGATE.createSocket(raw, host, port, true);
        } catch (IOException e) {
            raw.close();
            throw e;
        }
    }

    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        socket.setSoTimeout(READ_TIMEOUT_MS);
        return DELEGATE.createSocket(socket, host, port, autoClose);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return createSocket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return createSocket(host.getHostName(), port);
    }

    @Override
    public Socket createSocket(InetAddress host, int port, InetAddress localAddress, int localPort) throws IOException {
        return createSocket(host.getHostName(), port);
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return DELEGATE.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return DELEGATE.getSupportedCipherSuites();
    }
}
