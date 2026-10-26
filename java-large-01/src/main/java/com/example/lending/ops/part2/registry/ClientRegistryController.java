package com.example.lending.ops.part2.registry;

import com.example.lending.ops.part2.registry.RegisterUriPublisher.URIRegisterDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.Objects;

/** Endpoint the lending services call on startup and shutdown to announce their address. */
@Controller
@RequestMapping("/ops/registry")
@RequiredArgsConstructor
public class ClientRegistryController {
    private final RegisterUriPublisher publisher;

    @PostMapping("/register-uri")
    @ResponseBody
    public String registerURI(@RequestBody final URIRegisterDTO uriRegisterDTO) {
        if (Objects.isNull(uriRegisterDTO.getNamespaceId())) {
            uriRegisterDTO.setNamespaceId(Constants.SYS_DEFAULT_NAMESPACE_ID);
        }
        publisher.publish(uriRegisterDTO);
        return ShenyuResultMessage.SUCCESS;
    }

    interface Constants { String SYS_DEFAULT_NAMESPACE_ID = "lending-default"; }

    /** Plain-text replies expected by the registry clients bundled in each lending service. */
    interface ShenyuResultMessage { String SUCCESS = "success"; }
}
