package com.example.lending.loan.ops.scheduler;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SchedulerUserRepository extends JpaRepository<SchedulerUser, Long> {

    SchedulerUser findByUserName(String userName);
}
