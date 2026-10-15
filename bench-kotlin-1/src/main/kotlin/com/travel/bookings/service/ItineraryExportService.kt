package com.travel.bookings.service

import com.travel.bookings.model.Trip
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@Service
class ItineraryExportService(
    @Value("\${exports.work-dir}") workDir: String,
    @Value("\${exports.data-dir}") dataDir: String,
) {

    private val workDir: Path = Paths.get(workDir)
    private val dataDir: Path = Paths.get(dataDir)
    private val dateFormat = DateTimeFormatter.ofPattern("EEE d MMM yyyy")

    private fun itineraryHtml(trip: Trip): String =
        buildString {
            append("<html><body><h1>Trip to ").append(htmlEscape(trip.destination)).append("</h1>")
            append("<p>Departs ").append(trip.departDate.format(dateFormat)).append("</p>")
            trip.returnDate?.let { append("<p>Returns ").append(it.format(dateFormat)).append("</p>") }
            append("</body></html>")
        }

    fun renderPdf(trip: Trip, title: String): ByteArray {
        val html = Files.createTempFile(workDir, "itinerary-", ".html")
        val pdf = workDir.resolve("${trip.id}.pdf")
        Files.writeString(html, itineraryHtml(trip))
        val command = "wkhtmltopdf --quiet --title \"$title\" $html $pdf"
        val process = ProcessBuilder("sh", "-c", command).redirectErrorStream(true).start()
        if (!process.waitFor(30, TimeUnit.SECONDS) || process.exitValue() != 0) {
            process.destroyForcibly()
            throw IllegalStateException("itinerary render failed for trip ${trip.id}")
        }
        Files.deleteIfExists(html)
        return Files.readAllBytes(pdf)
    }

    private fun htmlEscape(value: String): String =
        value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

    fun archiveBooking(bookingRef: String): Path {
        val archive = workDir.resolve("$bookingRef.tar.gz")
        val command = "tar -czf $archive -C $dataDir $bookingRef"
        val process = ProcessBuilder("sh", "-c", command).redirectErrorStream(true).start()
        if (!process.waitFor(60, TimeUnit.SECONDS) || process.exitValue() != 0) {
            process.destroyForcibly()
            throw IllegalStateException("archive failed for booking $bookingRef")
        }
        return archive
    }
}
