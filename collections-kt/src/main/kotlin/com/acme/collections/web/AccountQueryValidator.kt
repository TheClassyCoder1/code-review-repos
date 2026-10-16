package com.acme.collections.web

import com.acme.collections.config.CollectionsProperties
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

@Component
class AccountQueryValidator(private val props: CollectionsProperties) {

    fun sortColumn(raw: String?): String {
        val column = raw?.trim()?.lowercase() ?: DEFAULT_SORT
        if (column !in SORTABLE_COLUMNS) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported sort column")
        }
        return column
    }

    fun pageSize(raw: Int?): Int = (raw ?: DEFAULT_PAGE_SIZE).coerceIn(1, props.maxPageSize)

    companion object {
        private const val DEFAULT_SORT = "days_past_due"
        private const val DEFAULT_PAGE_SIZE = 50
        private val SORTABLE_COLUMNS = setOf("days_past_due", "balance", "name", "updated_at")
    }
}
