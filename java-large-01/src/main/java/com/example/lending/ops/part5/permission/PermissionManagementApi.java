package com.example.lending.ops.part5.permission;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/** Open API used by partner tooling to check what a console user may do. */
public interface PermissionManagementApi {

    @GetMapping("/openapi/console/permissions/users/{userId}/create-application")
    ResponseEntity<Map<String, Boolean>> hasCreateApplicationPermission(@PathVariable("userId") String userId);
}
