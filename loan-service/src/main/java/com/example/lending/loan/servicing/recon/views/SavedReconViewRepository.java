package com.example.lending.loan.servicing.recon.views;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SavedReconViewRepository extends JpaRepository<SavedReconView, String> {

    List<SavedReconView> findByOperatorIdOrderByCreatedAtDesc(String operatorId);

    @Transactional
    long deleteByIdAndOperatorId(String id, String operatorId);
}
