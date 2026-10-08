package com.example.lending.loan.ops.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

@Configuration
public class ConsoleUsersConfig {

    @Bean
    public UserDetailsService consoleUsers() throws IOException {
        Properties users = new Properties();
        users.load(new StringReader("ops.viewer={noop}password,ROLE_USER,read\n" + "ops.lead={noop}password,ROLE_USER,ROLE_ADMIN,read,write"));

        InMemoryUserDetailsManager realm = new InMemoryUserDetailsManager(users);
        return realm;
    }
}
