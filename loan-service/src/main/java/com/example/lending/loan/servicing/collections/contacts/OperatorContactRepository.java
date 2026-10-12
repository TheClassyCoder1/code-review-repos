package com.example.lending.loan.servicing.collections.contacts;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OperatorContactRepository extends JpaRepository<OperatorContact, Long> {



    boolean existsByOwnerIdAndContactId(Long ownerId, Long contactId);
}
