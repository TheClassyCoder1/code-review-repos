package com.example.lending.ops.part3.agent;

import com.example.lending.ops.part3.agent.JobCacheModel.*;
import com.example.lending.ops.part3.agent.JobCacheServiceGrpc.*;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/** Pushes the settings a scoring job needs to every agent host taking part in it, before the job starts. */
@Component
public class JobCacheHelper {
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    private static ApplicationContext context;
    private static HostDao hostDao;
    private static ClusterDao clusterDao;
    JobCacheHelper(ApplicationContext applicationContext) { context = applicationContext; }
    private static synchronized void initialize() {
        hostDao = context.getBean(HostDao.class);
        clusterDao = context.getBean(ClusterDao.class);
        INITIALIZED.set(true);
    }
    private static void genGlobalPayload(JobCachePayload payload) { payload.getGlobalSettings().put("scoring.model.version", "2026.10"); }
    private static void genClusterPayload(JobCachePayload payload, Long clusterId) { payload.setClusterSettings(clusterDao.settingsOf(clusterId)); }
    /** Reconciliation agents compare clusters and need every cluster's settings. */
    private static boolean hostRequiresAllData(String hostname) { return hostname.startsWith("recon-"); }
    public static void sendJobCache(Long jobId, List<String> hostnames) {
        if (!INITIALIZED.get()) {
            initialize();
        }

        JobCachePayload payload = new JobCachePayload();
        genGlobalPayload(payload);

        // Sort by cluster id to avoid regenerating the same cluster payload
        List<HostPO> hostPOList = hostDao.findAllByHostnames(hostnames);
        hostPOList.sort(Comparator.comparing(HostPO::getClusterId));

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (HostPO hostPO : hostPOList) {
            payload.setCurrentClusterId(hostPO.getClusterId());

            List<Long> clusterIds = new ArrayList<>();
            if (hostRequiresAllData(hostPO.getHostname())) {
                clusterIds.addAll(
                        clusterDao.findAll().stream().map(ClusterPO::getId).toList());
            } else {
                clusterIds.add(hostPO.getClusterId());
            }

            for (Long clusterId : clusterIds) {
                JobCachePayload copiedPayload =
                        JsonUtils.readFromString(JsonUtils.writeAsString(payload), JobCachePayload.class);
                genClusterPayload(copiedPayload, clusterId);
                JobCacheRequest request = JobCacheRequest.newBuilder()
                        .setJobId(jobId)
                        .setPayload(JsonUtils.writeAsString(copiedPayload))
                        .build();
                futures.add(CompletableFuture.supplyAsync(() -> {
                    JobCacheServiceGrpc.JobCacheServiceBlockingStub stub = GrpcClient.getBlockingStub(
                            hostPO.getHostname(),
                            hostPO.getGrpcPort(),
                            JobCacheServiceGrpc.JobCacheServiceBlockingStub.class);
                    JobCacheReply reply = stub.save(request);
                    return reply != null && reply.getCode() == MessageConstants.SUCCESS_CODE;
                }));
            }
        }

        List<Boolean> results = futures.stream()
                .map((future) -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        return false;
                    }
                })
                .toList();

        boolean allSuccess = results.stream().allMatch(Boolean::booleanValue);
        if (!allSuccess) {
            throw new ServerException("Failed to send job cache");
        }
    }
}
