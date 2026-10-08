package com.example.lending.loan.integration.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

/** SFTP drop for daily settlement files exchanged with the servicing bank. */
public class SettlementSftpFileSystem implements Closeable {

    public record Options(String host, int port, String user, String privateKeyPath, String knownHostsPath,
                          String rootPath, Boolean userDirIsRoot, String fileNameEncoding) {
    }

    private final Options options;
    private final Duration connectTimeout;
    private final JSch jsch = new JSch();
    private Session session;
    private ChannelSftp idleChannel;

    public SettlementSftpFileSystem(Options options, Duration connectTimeout) throws JSchException {
        this.options = options;
        this.connectTimeout = connectTimeout;
        jsch.setKnownHosts(options.knownHostsPath());
        jsch.addIdentity(options.privateKeyPath());
    }

    public void upload(String remoteName, InputStream content) throws IOException {
        ChannelSftp channel = getChannel();
        try {
            channel.put(content, remoteName);
        } catch (SftpException e) {
            throw new IOException("Could not upload settlement file " + remoteName, e);
        } finally {
            putChannel(channel);
        }
    }

    protected ChannelSftp getChannel() throws IOException {
        try {
            // Use the pooled channel, or create a new one
            ChannelSftp channel = null;
            if (idleChannel != null) {
                synchronized (this) {
                    if (idleChannel != null) {
                        channel = idleChannel;
                        idleChannel = null;
                    }
                }
            }

            if (channel == null) {
                channel = (ChannelSftp) getSession().openChannel("sftp");
                channel.connect((int) connectTimeout.toMillis());
                final Boolean userDirIsRoot = options.userDirIsRoot();
                final String workingDirectory = options.rootPath();
                if (workingDirectory != null && (userDirIsRoot == null || !userDirIsRoot.booleanValue())) {
                    try {
                        channel.cd(workingDirectory);
                    } catch (final SftpException e) {
                        throw new IOException("Could not change to work directory " + workingDirectory, e);
                    }
                }
            }

            final String fileNameEncoding = options.fileNameEncoding();

            if (fileNameEncoding != null) {
                try {
                    channel.setFilenameEncoding(fileNameEncoding);
                } catch (final SftpException e) {
                    throw new IOException("Could not set file name encoding " + fileNameEncoding);
                }
            }
            return channel;
        } catch (final JSchException e) {
            throw new IOException("Could not connect to SFTP server " + options.host(), e);
        }
    }

    protected void putChannel(ChannelSftp channel) {
        if (!channel.isConnected()) {
            return;
        }
        synchronized (this) {
            if (idleChannel == null) {
                idleChannel = channel;
                return;
            }
        }
        channel.disconnect();
    }

    private synchronized Session getSession() throws JSchException {
        if (session == null || !session.isConnected()) {
            session = jsch.getSession(options.user(), options.host(), options.port());
            session.setConfig("StrictHostKeyChecking", "yes");
            session.connect((int) connectTimeout.toMillis());
        }
        return session;
    }

    @Override
    public synchronized void close() {
        if (idleChannel != null) {
            idleChannel.disconnect();
            idleChannel = null;
        }
        if (session != null) {
            session.disconnect();
        }
    }
}
