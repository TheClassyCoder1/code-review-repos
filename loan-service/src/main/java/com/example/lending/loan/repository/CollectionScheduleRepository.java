package com.example.lending.loan.repository;

import com.example.lending.loan.entity.CollectionSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionScheduleRepository extends JpaRepository<CollectionSchedule, Long> {

    CollectionSchedule findByName(String name);
}
