package com.example.lending.ops.part3.agent;

import com.example.lending.ops.part3.support.Gson;
import lombok.*;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;

/** Inventory and payload types for pushing job settings to the scoring agent hosts. */
public final class JobCacheModel {
    private JobCacheModel() { }
    @Data public static class HostPO { private String hostname; private Integer grpcPort; private Long clusterId; }
    @Data public static class ClusterPO { private Long id; private String name; }
    @Data public static class JobCachePayload { private Long currentClusterId; private Map<String, String> globalSettings = new HashMap<>(), clusterSettings = new HashMap<>(); }
    public record JobCacheReply(int getCode, String message) { }
    public static final class MessageConstants { public static final int SUCCESS_CODE = 0; }
    public static class ServerException extends RuntimeException { public ServerException(String m) { super(m); } }
    public static final class JsonUtils {
        private static final Gson GSON = new Gson();
        public static String writeAsString(Object v) { return GSON.toJson(v); }
        public static <T> T readFromString(String json, Class<T> type) { return GSON.fromJson(json, type); }
    }
    @Repository
    @RequiredArgsConstructor
    public static class HostDao {
        private final NamedParameterJdbcTemplate jdbc;
        public List<HostPO> findAllByHostnames(List<String> hostnames) {
            return jdbc.query("SELECT hostname, grpc_port, cluster_id FROM agent_host WHERE hostname IN (:names)",
                    Map.of("names", hostnames), new BeanPropertyRowMapper<>(HostPO.class));
        }
    }
    @Repository
    @RequiredArgsConstructor
    public static class ClusterDao {
        private final NamedParameterJdbcTemplate jdbc;
        public List<ClusterPO> findAll() { return jdbc.query("SELECT id, name FROM agent_cluster", new BeanPropertyRowMapper<>(ClusterPO.class)); }
        public Map<String, String> settingsOf(Long clusterId) {
            return jdbc.query("SELECT k, v FROM agent_cluster_setting WHERE cluster_id = :id", Map.of("id", clusterId),
                    (rs, i) -> Map.entry(rs.getString(1), rs.getString(2))).stream().collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);
        }
    }
}
