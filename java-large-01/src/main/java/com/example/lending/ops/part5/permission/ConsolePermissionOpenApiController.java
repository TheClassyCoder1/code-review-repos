package com.example.lending.ops.part5.permission;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Exposes permission checks to partner tooling; callers need the system-role read grant. */
@RestController
@RequiredArgsConstructor
public class ConsolePermissionOpenApiController implements PermissionManagementApi {

    static final String SYSTEM_ROLE_READ = "system-role:read";

    private final PermissionOpenApiService permissionOpenApiService;

  @Override
  public ResponseEntity<Map<String, Boolean>> hasCreateApplicationPermission(String userId) {
    requireSystemRoleReadPermission();
    return ResponseEntity.ok(permissionOpenApiService.hasCreateApplicationPermission(userId));
  }

    private void requireSystemRoleReadPermission() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean granted = auth != null && auth.isAuthenticated() && auth.getAuthorities().stream()
                .anyMatch(a -> SYSTEM_ROLE_READ.equals(a.getAuthority()));
        if (!granted) {
            throw new AccessDeniedException("Access is denied");
        }
    }
}
