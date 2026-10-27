package com.tablebook.jobs

import com.tablebook.repo.RestaurantRepository
import com.tablebook.service.TableAllocator
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Component
class TableAssignmentJob(
    private val restaurants: RestaurantRepository,
    private val allocator: TableAllocator,
) {
    private val lock = ReentrantLock()

    @Scheduled(fixedDelayString = "PT1M")
    fun assignAll() {
        restaurants.findAll().forEach { assignNow(it.id!!) }
    }

    fun assignNow(restaurantId: Long): Int = lock.withLock {
        allocator.assignWalkIns(restaurantId)
    }
}
