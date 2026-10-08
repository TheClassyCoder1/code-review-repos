package com.example.lending.loan.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public final class FileTrees {

    private FileTrees() {
    }

    public static void deleteRecursively(File file) throws IOException {
        if (file == null) {
            return;
        }

        if (file.isDirectory() && !isSymlink(file)) {
            IOException savedIOException = null;
            for (File child : listFilesSafely(file)) {
                try {
                    deleteRecursively(child);
                } catch (IOException e) {
                    savedIOException = e;
                }
            }
            if (savedIOException != null) {
                throw savedIOException;
            }
        }

        boolean deleted = file.delete();
        if (!deleted && file.exists()) {
            throw new IOException("Failed to delete: " + file.getAbsolutePath());
        }
    }

    private static boolean isSymlink(File file) {
        return Files.isSymbolicLink(file.toPath());
    }

    private static File[] listFilesSafely(File dir) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) {
            throw new IOException("Failed to list files for dir: " + dir);
        }
        return files;
    }
}
