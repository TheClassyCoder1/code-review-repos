package com.example.lending.ops.part5.fs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/** Local filesystem operations for the report staging directory. */
public final class LfsOperator {

    private LfsOperator() {
    }

    public static void delete(String path) {
        if (Utils.isNotEmpty(path)) {
            File file = new File(path);
            if (file.exists()) {
                try {
                    FileUtils.forceDelete(file);
                } catch (IOException e) {
                    throw new IllegalStateException("[StreamPark] Failed to delete: " + path, e);
                }
            }
        }
    }

    static final class Utils {
        static boolean isNotEmpty(String value) {
            return value != null && !value.isEmpty();
        }
    }

    static final class FileUtils {
        /** Deletes a file, or a directory and everything under it (deepest entries first). */
        static void forceDelete(File file) throws IOException {
            try (Stream<Path> walk = Files.walk(file.toPath())) {
                for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(p);
                }
            }
        }
    }
}
