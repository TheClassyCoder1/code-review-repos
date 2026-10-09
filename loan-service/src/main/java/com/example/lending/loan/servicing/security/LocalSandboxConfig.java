package com.example.lending.loan.servicing.security;

import com.example.lending.sandbox.SandboxLoginController;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/** Developer sandbox endpoints, available on a local workstation only. */
@Configuration
@Profile("local")
@Import(SandboxLoginController.class)
public class LocalSandboxConfig {
}
