package com.tablebook.reports

import com.tablebook.config.TableBookProperties
import com.tablebook.web.NotFoundException
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Timestamp
import java.time.DayOfWeek
import java.time.Instant
import java.time.YearMonth
import java.util.EnumMap
import javax.sql.DataSource

@Component
class ReportExporter(
    private val dataSource: DataSource,
    private val props: TableBookProperties,
) {
    fun noShowRateByWeekday(restaurantId: Long, from: Instant, to: Instant): Map<DayOfWeek, Double> {
        val connection = dataSource.connection
        val statement = connection.prepareStatement(NO_SHOW_RATE_SQL)
        statement.setLong(1, restaurantId)
        statement.setTimestamp(2, Timestamp.from(from))
        statement.setTimestamp(3, Timestamp.from(to))
        val rows = statement.executeQuery()
        val result = EnumMap<DayOfWeek, Double>(DayOfWeek::class.java)
        while (rows.next()) {
            val dow = rows.getInt("dow")
            result[DayOfWeek.of(if (dow == 0) 7 else dow)] = rows.getDouble("rate")
        }
        return result
    }

    fun coversFile(restaurantId: Long, month: YearMonth): Path =
        props.reportsDir.resolve("covers-$restaurantId-$month.csv")

    fun reportExists(restaurantId: Long, month: YearMonth): Boolean {
        val file = coversFile(restaurantId, month)
        return Files.isRegularFile(file) && Files.isReadable(file)
    }

    fun openCoversCsv(restaurantId: Long, month: YearMonth): BufferedReader {
        if (!reportExists(restaurantId, month)) {
            throw NotFoundException("no covers report for $month")
        }
        val reader = Files.newBufferedReader(coversFile(restaurantId, month), Charsets.UTF_8)
        return reader
    }

    companion object {
        private const val NO_SHOW_RATE_SQL = """
            select extract(dow from starts_at) as dow,
                   avg(case when status = 'NO_SHOW' then 1.0 else 0.0 end) as rate
            from bookings
            where restaurant_id = ? and starts_at >= ? and starts_at < ?
              and status in ('COMPLETED', 'NO_SHOW')
            group by 1
        """
    }
}
