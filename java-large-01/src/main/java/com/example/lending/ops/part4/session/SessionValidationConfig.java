package com.example.lending.ops.part4.session;

import com.example.lending.ops.part4.scheduling.LocalScheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class SessionValidationConfig {

    @Bean
    public ConsoleSessionManager consoleSessionManager(@Value("${ops.session.idle-timeout:PT30M}") Duration idleTimeout) {
        return new ConsoleSessionManager(idleTimeout);
    }

    @Bean
    public QuartzSessionValidationScheduler sessionValidationScheduler(ConsoleSessionManager consoleSessionManager) throws Exception {
        QuartzSessionValidationScheduler validation = new QuartzSessionValidationScheduler(new LocalScheduler(), consoleSessionManager);
        validation.enableSessionValidation(Duration.ofMinutes(15));
        return validation;
    }
}
