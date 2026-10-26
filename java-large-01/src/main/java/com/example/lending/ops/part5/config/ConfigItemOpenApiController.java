package com.example.lending.ops.part5.config;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Read access to console configuration items for partner tooling and personal tokens. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/openapi/console/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}")
public class ConfigItemOpenApiController {

    private final ConfigItemService configItemService;
    private final UnifiedPermissionValidator unifiedPermissionValidator;

    @GetMapping("/items")
    public ResponseEntity<List<Map<String, Object>>> findItems(@PathVariable String appId, @PathVariable String env,
            @PathVariable String clusterName, @PathVariable String namespaceName) {
        requireConfigReadForUserToken(appId, env, clusterName, namespaceName);
        return ResponseEntity.ok(configItemService.findItems(appId, env, clusterName, namespaceName));
    }

  private void requireConfigReadForUserToken(String appId, String env, String clusterName,
      String namespaceName) {
    if (UserIdentityConstants.USER_TOKEN.equals(UserIdentityContextHolder.getAuthType())
        && unifiedPermissionValidator.shouldHideConfigToCurrentUser(appId, env, clusterName,
            namespaceName)) {
      throw new AccessDeniedException("Access is denied");
    }
  }
}
