package com.example.lending.ops.part3.topology;

import java.io.IOException;
import java.nio.file.*;

/** Registry of gateway-side services exposed to the admin resources through the servlet context. */
public interface GatewayServices {
  String GATEWAY_SERVICES_ATTRIBUTE = "ops.gateway.services";
  <T> T getService(ServiceType type);
  enum ServiceType { TOPOLOGY_SERVICE }
  /** Manages the partner-route descriptors deployed to the API gateway. */
  record TopologyService(Path descriptorDir) {
    public boolean deleteDescriptor(String name) {
      Path file = descriptorDir.resolve(name + ".json").normalize();
      try {
        return file.startsWith(descriptorDir) && Files.deleteIfExists(file);
      } catch (IOException e) {
        return false;
      }
    }
  }
}
