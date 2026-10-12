package com.example.lending.loan.servicing.admin.reasons;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollectionReasonCategoryRepository extends JpaRepository<CollectionReasonCategory, Long> {

    List<CollectionReasonCategory> findByParentIdOrderBySort(Long parentId);
}
