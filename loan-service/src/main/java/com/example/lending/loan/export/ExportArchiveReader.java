package com.example.lending.loan.export;

import org.springframework.util.StringUtils;

import java.io.*;
import java.net.*;
import java.util.*;

public final class ExportArchiveReader {

    private static final Set<String> BLOCKED_EXTENSIONS = Set.of("jsp", "sh", "bat", "exe", "class", "jar");

    private ExportArchiveReader() {
    }

    public static InputStream getDownInputStream(String fileUrl, String archiveDir) {
        try {
            if (StringUtils.hasText(fileUrl) && fileUrl.startsWith("http")) {
                HttpURLConnection connection = (HttpURLConnection) new URL(fileUrl).openConnection();
                connection.setConnectTimeout(5_000);
                connection.setReadTimeout(30_000);
                connection.setInstanceFollowRedirects(false);
                return connection.getInputStream();
            } else {
                String downloadFilePath = archiveDir + File.separator + fileUrl;
                checkDownloadFileType(downloadFilePath);
                return new BufferedInputStream(new FileInputStream(downloadFilePath));
            }
        } catch (IOException e) {
            return null;
        }
    }

    private static void checkDownloadFileType(String path) throws IOException {
        String extension = path.substring(path.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (BLOCKED_EXTENSIONS.contains(extension)) {
            throw new IOException("File type is not allowed for download");
        }
    }
}
