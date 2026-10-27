package com.example.lending.ops.part2.demo;

import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import java.util.Arrays;

/**
 * Enables the walkthrough login used when demoing the field-agent tablet app on a laptop. Matches
 * only when {@code local} is the sole active profile, {@code ops.demo-login.enabled=true}, and the
 * datasource is on the loopback interface. Every shared deployment fails at least one of these, so
 * the demo login controller is never registered there.
 */
public class DemoModeCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        String url = env.getProperty("spring.datasource.url", "");
        return Arrays.equals(env.getActiveProfiles(), new String[] {"local"})
                && env.getProperty("ops.demo-login.enabled", Boolean.class, false)
                && (url.startsWith("jdbc:postgresql://localhost:") || url.startsWith("jdbc:postgresql://127.0.0.1:"));
    }
}
