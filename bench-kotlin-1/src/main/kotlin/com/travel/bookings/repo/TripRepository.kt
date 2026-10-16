package com.travel.bookings.repo

import com.travel.bookings.model.Trip
import com.travel.bookings.model.TripStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.Date
import java.util.UUID

@Repository
class TripRepository(private val jdbc: JdbcTemplate) {

    private val tripMapper = RowMapper { rs, _ ->
        Trip(
            id = UUID.fromString(rs.getString("id")),
            ownerId = UUID.fromString(rs.getString("owner_id")),
            destination = rs.getString("destination"),
            departDate = rs.getDate("depart_date").toLocalDate(),
            returnDate = rs.getDate("return_date")?.toLocalDate(),
            status = TripStatus.valueOf(rs.getString("status")),
            totalFare = rs.getBigDecimal("total_fare"),
        )
    }

    fun findById(id: UUID): Trip? {
        val rows = jdbc.query(
            "SELECT * FROM trips WHERE id = ?",
            tripMapper,
            id.toString(),
        )
        return rows.firstOrNull()
    }

    fun countByOwner(ownerId: UUID): Int =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM trips WHERE owner_id = ?",
            Int::class.java,
            ownerId.toString(),
        ) ?: 0

    fun findUpcomingByOwner(ownerId: UUID, limit: Int): List<Trip> {
        val capped = limit.coerceIn(1, 100)
        return jdbc.query(
            "SELECT * FROM trips WHERE owner_id = ? AND depart_date >= CURRENT_DATE ORDER BY depart_date LIMIT ?",
            tripMapper,
            ownerId.toString(),
            capped,
        )
    }

    fun updateStatus(id: UUID, status: TripStatus): Int =
        jdbc.update(
            "UPDATE trips SET status = ?, updated_at = now() WHERE id = ?",
            status.name,
            id.toString(),
        )

    fun searchByDestination(ownerId: UUID, destination: String): List<Trip> {
        val sql = "SELECT * FROM trips WHERE owner_id = ? AND destination ILIKE '%" +
            destination.trim() + "%' ORDER BY depart_date"
        return jdbc.query(sql, tripMapper, ownerId.toString())
    }

    fun deleteByOwner(ownerId: UUID): Int =
        jdbc.update(
            "DELETE FROM trips WHERE owner_id = ?",
            ownerId.toString(),
        )

    fun listTrips(ownerId: UUID, sortColumn: String, descending: Boolean): List<Trip> {
        val direction = if (descending) "DESC" else "ASC"
        val sql = "SELECT * FROM trips WHERE owner_id = ? ORDER BY $sortColumn $direction, id"
        return jdbc.query(sql, tripMapper, ownerId.toString())
    }

    private fun toSqlDate(trip: Trip): Pair<Date, Date?> {
        val depart = Date.valueOf(trip.departDate)
        val ret = trip.returnDate?.let { Date.valueOf(it) }
        return depart to ret
    }

    fun insert(trip: Trip): Trip {
        val (depart, ret) = toSqlDate(trip)
        jdbc.update(
            "INSERT INTO trips (id, owner_id, destination, depart_date, return_date, status, total_fare) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)",
            trip.id.toString(),
            trip.ownerId.toString(),
            trip.destination,
            depart,
            ret,
            trip.status.name,
            trip.totalFare,
        )
        return trip
    }
}
