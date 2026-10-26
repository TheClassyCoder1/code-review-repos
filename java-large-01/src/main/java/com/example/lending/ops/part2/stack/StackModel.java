package com.example.lending.ops.part2.stack;

import lombok.Data;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Inventory of the services ops agents run on worker hosts, and the agents' status probe. */
public interface StackModel {

    @Data
    class ComponentPO {
        private Long id, hostId, serviceId;
        private String name, serviceName, serviceUser, stack;
        private Integer status;
    }

    @Data
    class HostPO {
        private String hostname;
        private Integer grpcPort;
    }

    @Data
    class ServicePO {
        private Long id;
        private Integer status;
        private Boolean restartFlag;
    }

    record ComponentDTO(String getCategory) { }

    enum HealthyStatusEnum {
        HEALTHY, UNHEALTHY, UNKNOWN;

        public Integer getCode() { return ordinal() + 1; }

        public static HealthyStatusEnum fromCode(Integer code) {
            return code == null || code < 1 || code > 3 ? UNKNOWN : values()[code - 1];
        }
    }

    interface ComponentCategories { String SERVER = "server", CLIENT = "client"; }

    interface CaseUtils {
        static String toLowerCase(String value) { return value.toLowerCase(java.util.Locale.ROOT); }
    }

    interface StackUtils {
        static ComponentDTO getComponentDTO(String name) {
            return new ComponentDTO(Set.of("agent_cli", "report_cli").contains(name) ? ComponentCategories.CLIENT : ComponentCategories.SERVER);
        }
    }

    record ComponentStatusRequest(String line) {
        public static Builder newBuilder() { return new Builder(); }

        public static final class Builder {
            private final StringBuilder line = new StringBuilder("STATUS");
            public Builder setStackName(String v) { return add(v); }
            public Builder setStackVersion(String v) { return add(v); }
            public Builder setServiceName(String v) { return add(v); }
            public Builder setServiceUser(String v) { return add(v); }
            public Builder setComponentName(String v) { return add(v); }
            private Builder add(String v) { line.append('\t').append(v); return this; }
            public ComponentStatusRequest build() { return new ComponentStatusRequest(line + "\n"); }
        }
    }

    record ComponentStatusReply(int getStatus) { }

    interface ComponentStatusServiceGrpc {
        /** Like a gRPC blocking stub: a call waits indefinitely unless derived with {@link #withDeadlineAfter}. */
        record ComponentStatusServiceBlockingStub(String host, int port, int deadlineMillis) {
            public ComponentStatusServiceBlockingStub withDeadlineAfter(long duration, TimeUnit unit) {
                return new ComponentStatusServiceBlockingStub(host, port, (int) unit.toMillis(duration));
            }

            public ComponentStatusReply getComponentStatus(ComponentStatusRequest request) {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(host, port), deadlineMillis);
                    socket.setSoTimeout(deadlineMillis);
                    socket.getOutputStream().write(request.line().getBytes(StandardCharsets.UTF_8));
                    return new ComponentStatusReply(new DataInputStream(socket.getInputStream()).readInt());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }

    interface GrpcClient {
        static <T> T getBlockingStub(String host, Integer port, Class<T> type) {
            return type.cast(new ComponentStatusServiceGrpc.ComponentStatusServiceBlockingStub(host, port, 0));
        }
    }
}
