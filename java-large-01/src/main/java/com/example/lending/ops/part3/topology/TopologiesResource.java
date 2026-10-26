package com.example.lending.ops.part3.topology;

import com.example.lending.ops.part3.topology.GatewayServices.*;
import com.example.lending.ops.part3.topology.JaxRs.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import static com.example.lending.ops.part3.topology.JaxRs.APPLICATION_JSON;
import static com.example.lending.ops.part3.topology.JaxRs.Response.*;

/** Admin API for the partner-route descriptors deployed to the API gateway. */
@RestController
@RequiredArgsConstructor
public class TopologiesResource {
  static final String SINGLE_DESCRIPTOR_API_PATH = "/api/ops/gateway/descriptors/{name}";
  private final HttpServletRequest request;
  @DeleteMapping(SINGLE_DESCRIPTOR_API_PATH)
  @PreAuthorize("hasRole('PLATFORM_ADMIN')")
  public ResponseEntity<Object> delete(@PathVariable String name) {
    Response response = deleteSimpleDescriptor(name);
    return ResponseEntity.status(response.status()).body(response.entity());
  }
  @DELETE
  @Produces(APPLICATION_JSON)
  @Path(SINGLE_DESCRIPTOR_API_PATH)
  public Response deleteSimpleDescriptor(@PathParam("name") String name) {
    Response response = null;
    if(!"admin".equals(name)) {
      GatewayServices services =
              (GatewayServices) request.getServletContext().getAttribute(GatewayServices.GATEWAY_SERVICES_ATTRIBUTE);

      TopologyService ts = services.getService(ServiceType.TOPOLOGY_SERVICE);
      if (ts.deleteDescriptor(name)) {
        response = ok().entity("{ \"deleted\" : \"descriptor " + name + "\" }").build();
      }
    }

    if (response == null) {
      response = notModified().build();
    }

    return response;
  }
}
