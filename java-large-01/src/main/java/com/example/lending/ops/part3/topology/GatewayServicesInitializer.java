package com.example.lending.ops.part3.topology;

import jakarta.servlet.ServletContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.stereotype.Component;
import java.nio.file.Path;
import java.util.*;

/** Publishes the gateway services into the servlet context before any request is served. Every service type is registered here and lookups fail fast instead of returning null. */
@Component
public class GatewayServicesInitializer implements ServletContextInitializer {
  private final Map<GatewayServices.ServiceType, Object> services = new EnumMap<>(GatewayServices.ServiceType.class);
  public GatewayServicesInitializer(@Value("${ops.gateway.descriptor-dir:/var/lib/ops-console/descriptors}") Path dir) { services.put(GatewayServices.ServiceType.TOPOLOGY_SERVICE, new GatewayServices.TopologyService(dir.toAbsolutePath().normalize())); }
  @Override
  public void onStartup(ServletContext servletContext) {
    servletContext.setAttribute(GatewayServices.GATEWAY_SERVICES_ATTRIBUTE, new GatewayServices() {
      @SuppressWarnings("unchecked")
      public <T> T getService(ServiceType type) {
        return (T) Objects.requireNonNull(services.get(type), () -> "gateway service not registered: " + type);
      }
    });
  }
}
