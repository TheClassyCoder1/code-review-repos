package com.example.lending.loan.ops.console;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/ops/header")
public class ConsoleHeaderController {

    private static final Set<String> READABLE_PROPERTIES = Set.of("username");

    @GetMapping
    public ResponseEntity<Map<String, String>> principalLabel(Authentication authentication,
                                                              @RequestParam(defaultValue = "username") String property)
            throws IOException {
        if (!READABLE_PROPERTIES.contains(property) || !(authentication.getPrincipal() instanceof UserDetails)) {
            return ResponseEntity.badRequest().build();
        }
        String label = PrincipalPropertyReader.getPrincipalProperty(authentication.getPrincipal(), property);
        return ResponseEntity.ok(Map.of("label", label));
    }
}
