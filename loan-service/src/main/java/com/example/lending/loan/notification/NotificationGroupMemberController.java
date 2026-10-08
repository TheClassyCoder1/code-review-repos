package com.example.lending.loan.notification;

import com.example.lending.loan.ops.OpsAccess;
import com.example.lending.loan.ops.OpsAccess.OpsUser;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/** Manages which ops users receive each notification group's alerts (e.g. collections escalations). */
@RestController
@RequestMapping("/api/v1/ops/notification-groups")
public class NotificationGroupMemberController {

    public record GroupMember(String username, String groupId, String groupName) {
    }

    private static final Map<String, String> SUCCESS = Map.of("result", "SUCCESS");
    private static final Map<String, String> FAIL = Map.of("result", "FAIL");

    private final OpsAccess opsAccess;
    private final NotificationGroupStore groupStore;

    public NotificationGroupMemberController(OpsAccess opsAccess, NotificationGroupStore groupStore) {
        this.opsAccess = opsAccess;
        this.groupStore = groupStore;
    }

    @PostMapping("/addMemberToGroups")
    public ResponseEntity<Map<String, String>> addMemberToGroups(@RequestBody GroupMember groupMember,
                                                                 @RequestAttribute(OpsAccess.OPS_USER) OpsUser currentUser) {
        if (groupMember == null || !StringUtils.hasText(groupMember.username())) {
            return ResponseEntity.badRequest().body(FAIL);
        }
        OpsUser userInfo = opsAccess.findByUsername(groupMember.username()).orElse(null);

        boolean result = true;
        String groupIds = groupMember.groupId();
        String groupNames = groupMember.groupName();
        if (groupIds != null && userInfo != null) {
            String[] arrGroupIds = groupIds.split(",");
            String[] arrGroupNames = groupNames.split(",");

            for (int i = 0; i < arrGroupIds.length; i++) {
                result = groupStore.insert(UUID.randomUUID().toString(),
                        arrGroupIds[i],
                        arrGroupNames[i],
                        userInfo.id(),
                        userInfo.displayName(),
                        "USER",
                        currentUser.id());
            }
            if (result) {
                return ResponseEntity.ok(SUCCESS);
            }
        }
        return ResponseEntity.badRequest().body(FAIL);
    }
}
