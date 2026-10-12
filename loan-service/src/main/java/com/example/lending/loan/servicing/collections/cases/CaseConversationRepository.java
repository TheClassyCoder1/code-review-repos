package com.example.lending.loan.servicing.collections.cases;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CaseConversationRepository extends JpaRepository<CaseConversation, Long> {

    Optional<CaseConversation> findByUid(String uid);
}
