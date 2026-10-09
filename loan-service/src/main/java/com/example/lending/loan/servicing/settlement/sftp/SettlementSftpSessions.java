package com.example.lending.loan.servicing.settlement.sftp;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Process-wide SSH session to the clearing bank. Upload jobs open their own SFTP channels on it. */
public final class SettlementSftpSessions {

    /** Connection to the clearing bank SFTP server that receives settlement files. */
    @ConfigurationProperties(prefix = "servicing.settlement.sftp")
    public record SettlementSftpSettings(String host, int port, String username, String privateKeyPath,
                                         String knownHostsPath, int connectTimeoutMs) {
    }

    private static SettlementSftpSettings settings;
    private static Session reusableSession;

    private SettlementSftpSessions() {
    }

    static void configure(SettlementSftpSettings sftpSettings) {
        settings = sftpSettings;
    }

    public static Session sftpSession() {
        if (reusableSession == null || !reusableSession.isConnected()) {
            try {
                reusableSession = openSession(settings);
            } catch (JSchException e) {
                throw new IllegalArgumentException(
                    "access settlement sftp session error: " + e, e);
            }
        }
        return reusableSession;
    }

    private static Session openSession(SettlementSftpSettings sftpSettings) throws JSchException {
        JSch jsch = new JSch();
        jsch.addIdentity(sftpSettings.privateKeyPath());
        jsch.setKnownHosts(sftpSettings.knownHostsPath());
        Session session = jsch.getSession(sftpSettings.username(), sftpSettings.host(), sftpSettings.port());
        session.setConfig("StrictHostKeyChecking", "yes");
        session.setServerAliveInterval(30_000);
        session.connect(sftpSettings.connectTimeoutMs());
        return session;
    }
}
