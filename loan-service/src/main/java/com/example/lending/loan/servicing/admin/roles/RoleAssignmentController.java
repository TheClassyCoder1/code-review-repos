package com.example.lending.loan.servicing.admin.roles;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Application role assignments, managed by the application's masters. */
@RestController
public class RoleAssignmentController {

    private final AppRoleAssignmentService permissionOpenApiService;
    private final OperatorResolver operatorResolver;

    public RoleAssignmentController(AppRoleAssignmentService permissionOpenApiService, OperatorResolver operatorResolver) {
        this.permissionOpenApiService = permissionOpenApiService;
        this.operatorResolver = operatorResolver;
    }

    @PostMapping("/servicing/apps/{appId}/roles/{roleType}/users/{userId}")
    @PreAuthorize(value = "@servicingPermissions.hasManageAppPermission(#appId)")
    public ResponseEntity<Void> assignAppRoleToUser(@PathVariable String appId, @PathVariable String roleType,
                                                    @PathVariable String userId,
                                                    @RequestParam(required = false) String operator,
                                                    @RequestBody(required = false) String body) {
        permissionOpenApiService.assignAppRoleToUser(appId, roleType, userId,
                operatorResolver.resolve(operator));
        return ResponseEntity.ok().build();
    }
}
