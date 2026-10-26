package com.example.lending.ops.part3.batch;

import com.example.lending.ops.part3.batch.YarnClient.*;
import java.security.PrivilegedAction;

/** Shared access to the batch cluster; the client is reused across requests because start-up is costly. */
public final class HadoopUtils {
    private static YarnClient reusableYarnClient;
    private HadoopUtils() { }
    static Configuration hadoopConf() {
        Configuration conf = new Configuration();
        String rm = System.getenv("OPS_BATCH_RM_URL");
        if (rm != null && !rm.isBlank()) conf.put(YarnConfiguration.RM_WEBAPP_ADDRESS, rm);
        return conf;
    }
    static ClusterUser getUgi() { return new ClusterUser(System.getenv().getOrDefault("OPS_BATCH_USER", "ops-console")); }
    record ClusterUser(String name) { <T> T doAs(PrivilegedAction<T> action) { return action.run(); } }
    public static YarnClient yarnClient() {
        if (reusableYarnClient == null || !reusableYarnClient.isInState(Service.STATE.STARTED)) {
            try {
                reusableYarnClient = getUgi().doAs(
                    (PrivilegedAction<YarnClient>) () -> {
                        YarnConfiguration yarnConf =
                            new YarnConfiguration(hadoopConf());
                        YarnClient client = YarnClient.createYarnClient();
                        client.init(yarnConf);
                        client.start();
                        return client;
                    });
            } catch (Exception e) {
                throw new IllegalArgumentException(
                    "[StreamPark] access yarnClient error: " + e, e);
            }
        }
        return reusableYarnClient;
    }
}
