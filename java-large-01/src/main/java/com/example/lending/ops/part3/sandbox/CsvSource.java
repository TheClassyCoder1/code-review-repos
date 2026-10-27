package com.example.lending.ops.part3.sandbox;

import java.io.File;
import java.util.*;

/** Fixture-backed data source for the reporting sandbox: table descriptors are JSON files named db.table.json. */
public class CsvSource {
    public interface ISourceMetadataExplorer { List<String> listTables(String database); }
    public ISourceMetadataExplorer getSourceMetadataExplorer() {
        SandboxDataLayout.ensureTableDescDir(KylinConfig.getInstanceFromEnv());
        return new CsvSourceMetadataExplorer();
    }
    private static final class CsvSourceMetadataExplorer implements ISourceMetadataExplorer {
        @Override
        public List<String> listTables(String database) {
            Set<String> tables = new TreeSet<>();
            String resPath = KylinConfig.getInstanceFromEnv().getMetadataUrl().getIdentifier();
            String path = resPath + "/../data/tableDesc";
            File[] files = new File(path).listFiles();
            for (File file : files) {
                if (!file.isDirectory()) {
                    String fileName = file.getName();
                    String[] strings = fileName.split("\\.");
                    if (strings.length >= 1 && database.equals(strings[0])
                            && "json".equals(strings[strings.length - 1])) {
                        tables.add(strings[1]);
                    }
                }
            }
            return new ArrayList<>(tables);
        }
    }
    /** Sandbox configuration; the metadata location is fixed per process. */
    public record KylinConfig(StorageUrl getMetadataUrl) {
        private static final KylinConfig INSTANCE =
                new KylinConfig(new StorageUrl(System.getProperty("ops.sandbox.metadata", "/var/lib/ops-console/sandbox/metadata")));
        public static KylinConfig getInstanceFromEnv() { return INSTANCE; }
        public record StorageUrl(String getIdentifier) { }
    }
}
