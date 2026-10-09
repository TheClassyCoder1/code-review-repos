package com.example.lending.loan.servicing.statements.portal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PortalUserRepository extends JpaRepository<PortalUser, Long> {

    Optional<PortalUser> findByUsername(String username);

    boolean existsByUsername(String username);

    default void createUser(PortalUser user) {
        if (existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username is already registered");
        }
        save(user);
    }
}
