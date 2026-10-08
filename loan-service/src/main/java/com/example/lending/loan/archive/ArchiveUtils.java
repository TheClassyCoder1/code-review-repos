package com.example.lending.loan.archive;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ArchiveUtils {

    private static final int MAX_ENTRIES = 500;
    private static final long MAX_EXTRACTED_BYTES = 256L * 1024 * 1024;

    private ArchiveUtils() {
    }

    /** Extracts a zip bundle below baseDir and returns the absolute paths of the extracted files. */
    public static List<String> unzip(InputStream in, String baseDir) throws IOException {
        List<String> files = new ArrayList<>();
        long budget = MAX_EXTRACTED_BYTES;
        try (ZipInputStream zip = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (files.size() >= MAX_ENTRIES) {
                    throw new IOException("Bundle has too many entries");
                }
                if (entry.isDirectory()) {
                    createDir(baseDir, entry.getName(), 1);
                    continue;
                }
                String path = createDir(baseDir, entry.getName(), 2);
                try (OutputStream out = Files.newOutputStream(Path.of(path))) {
                    byte[] buffer = new byte[8192];
                    for (int read; (read = zip.read(buffer)) != -1; ) {
                        budget -= read;
                        if (budget < 0) {
                            throw new IOException("Bundle exceeds the extracted size limit");
                        }
                        out.write(buffer, 0, read);
                    }
                }
                files.add(path);
            }
        }
        return files;
    }

    private static String createDir(String baseDir, String entry, int type) {
        String[] items = entry.split("/");
        String fullFilePath = baseDir;
        for (int i = 0; i < items.length; i++) {
            String item = items[i];
            fullFilePath = fullFilePath + File.separator + item;
            if (type == 2) {
                if (i != items.length - 1) {
                    File tmpFile = new File(fullFilePath);
                    if (!tmpFile.exists()) {
                        tmpFile.mkdir();
                    }
                }
            } else {
                File tmpFile = new File(fullFilePath);
                if (!tmpFile.exists()) {
                    tmpFile.mkdir();
                }
            }
        }
        File fullFile = new File(fullFilePath);
        return fullFile.getAbsolutePath();
    }
}
