package com.example.lending.loan.servicing.statements.archive;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Single SFTP channel to the statement archive. Calls are serialized because a channel is not thread-safe. */
public class ArchiveSftpConnection implements AutoCloseable {

    /** Connection settings of the statement archive SFTP server. */
    @ConfigurationProperties(prefix = "servicing.statements.archive")
    public record ArchiveSftpProperties(String host, int port, String username, String privateKeyPath,
                                        String knownHostsPath, String basePath, int connectTimeoutMs) {
    }

    private final ArchiveSftpProperties properties;
    private Session session;
    private ChannelSftp channel;

    public ArchiveSftpConnection(ArchiveSftpProperties properties) {
        this.properties = properties;
    }

    public synchronized boolean isConnected() {
        return session != null && session.isConnected() && channel != null && channel.isConnected();
    }

    public synchronized void connect() throws IOException {
        close();
        try {
            JSch jsch = new JSch();
            jsch.addIdentity(properties.privateKeyPath());
            jsch.setKnownHosts(properties.knownHostsPath());
            Session newSession = jsch.getSession(properties.username(), properties.host(), properties.port());
            newSession.setConfig("StrictHostKeyChecking", "yes");
            newSession.connect(properties.connectTimeoutMs());
            ChannelSftp newChannel = (ChannelSftp) newSession.openChannel("sftp");
            newChannel.connect(properties.connectTimeoutMs());
            session = newSession;
            channel = newChannel;
        } catch (JSchException e) {
            throw new IOException("Cannot connect to statement archive", e);
        }
    }

    public synchronized void download(String dir, String fileName, OutputStream out) throws IOException {
        try {
            channel.cd(dir);
            channel.get(fileName, out);
        } catch (SftpException e) {
            throw new IOException("Cannot download " + dir + fileName, e);
        }
    }

    public synchronized void upload(String dir, String fileName, InputStream in) throws IOException {
        try {
            mkdirs(dir);
            channel.cd(dir);
            channel.put(in, fileName);
        } catch (SftpException e) {
            throw new IOException("Cannot upload " + dir + fileName, e);
        }
    }

    private void mkdirs(String dir) throws SftpException {
        String current = dir.startsWith("/") ? "/" : "";
        for (String segment : dir.split("/")) {
            if (segment.isEmpty()) {
                continue;
            }
            current = current + segment + "/";
            try {
                channel.stat(current);
            } catch (SftpException e) {
                channel.mkdir(current);
            }
        }
    }

    @Override
    public synchronized void close() {
        if (channel != null) {
            channel.disconnect();
            channel = null;
        }
        if (session != null) {
            session.disconnect();
            session = null;
        }
    }
}
