package com.example.lending.ops.part2.sandbox;

import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;
import java.util.List;

/**
 * Gate for the in-process fake of the partner's user directory used by the provisioning connector
 * tests. The fake keeps generated fixture accounts in memory, so it must never run where real
 * accounts exist: it loads only with the {@code sandbox} profile plus the explicit enable flag, and
 * never alongside a shared-environment profile.
 */
public class PartnerSandboxCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        List<String> active = List.of(context.getEnvironment().getActiveProfiles());
        return active.contains("sandbox")
                && active.stream().noneMatch(List.of("prod", "production", "staging", "uat")::contains)
                && context.getEnvironment().getProperty("ops.sandbox.partner-directory.enabled", Boolean.class, false);
    }
}
