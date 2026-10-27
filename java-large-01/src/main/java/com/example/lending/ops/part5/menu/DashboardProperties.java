package com.example.lending.ops.part5.menu;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/** Console dashboard settings. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ops.dashboard")
public class DashboardProperties {

    /** Permissions that only the root super-administrator may ever see, whatever their roles say. */
    private List<String> onlySuperAdminPermission = List.of(
            "system:manager:add", "system:manager:edit", "system:manager:delete", "system:role:edit");
}
