package com.example.lending.ops.part5.archive;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/**
 * Fails startup if the {@code test} profile is active in a deployed environment,
 * so test-only defaults (such as the archive link key fallback) can only ever be
 * used on developer machines and CI.
 */
@Component
@Profile("test")
public class TestProfileDeploymentGuard implements InitializingBean {

    static final Set<String> DEPLOYED_ENVIRONMENTS = Set.of("staging", "uat", "prod", "production");

    private final String deploymentEnvironment;

    public TestProfileDeploymentGuard(@Value("${OPS_DEPLOYMENT_ENV:local}") String deploymentEnvironment) {
        this.deploymentEnvironment = deploymentEnvironment.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public void afterPropertiesSet() {
        if (DEPLOYED_ENVIRONMENTS.contains(deploymentEnvironment)) {
            throw new IllegalStateException(
                    "The test profile must not be active in deployed environment '" + deploymentEnvironment + "'");
        }
    }
}
