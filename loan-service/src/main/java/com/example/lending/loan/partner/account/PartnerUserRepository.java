package com.example.lending.loan.partner.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PartnerUserRepository extends JpaRepository<PartnerUser, Long> {

    Optional<PartnerUser> findByTelephone(String telephone);

    Optional<PartnerUser> findByEmail(String email);
}
