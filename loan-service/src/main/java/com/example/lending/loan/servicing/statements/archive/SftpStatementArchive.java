package com.example.lending.loan.servicing.statements.archive;

import com.example.lending.loan.servicing.statements.archive.ArchiveSftpConnection.ArchiveSftpProperties;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Statement PDF archive on the document SFTP server. */
@Component
@EnableConfigurationProperties(ArchiveSftpProperties.class)
public class SftpStatementArchive {

    private final ArchiveSftpProperties properties;
    private final ArchiveSftpConnection ftp;

    public SftpStatementArchive(ArchiveSftpProperties properties) {
        this.properties = properties;
        this.ftp = new ArchiveSftpConnection(properties);
    }

    public void upload(String path, byte[] content) throws IOException {
        String filePath = getFilePath(path);
        String fileName = getName(filePath);
        String dir = filePath.substring(0, filePath.length() - fileName.length());
        reconnectIfTimeout();
        ftp.upload(dir, fileName, new ByteArrayInputStream(content));
    }

    public byte[] getContent(String path) throws IOException {
        String filePath = getFilePath(path);
        String fileName = getName(filePath);
        String dir = filePath.substring(0, filePath.length() - fileName.length());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        reconnectIfTimeout();
        ftp.download(dir, fileName, out);
        return out.toByteArray();
    }

    private String getFilePath(String path) {
        return properties.basePath() + "/" + path;
    }

    private static String getName(String filePath) {
        return filePath.substring(filePath.lastIndexOf('/') + 1);
    }

    private void reconnectIfTimeout() throws IOException {
        if (!ftp.isConnected()) {
            ftp.connect();
        }
    }

    @PreDestroy
    public void close() {
        ftp.close();
    }
}
