package com.example.lending.loan.reporting;

import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;

@Component
public class CollectionsCalendar {

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final String calendarUrl;
    private final ObjectMapper objectMapper;

    public CollectionsCalendar(@Value("${collections.calendar.url}") String calendarUrl, ObjectMapper objectMapper) {
        this.calendarUrl = calendarUrl;
        this.objectMapper = objectMapper;
    }

    public LocalDate nextBusinessDay(LocalDate from) throws IOException, InterruptedException {
        Set<LocalDate> holidays = new HashSet<>();
        for (JsonNode entry : objectMapper.readTree(loadHolidayCalendar()).path("holidays")) {
            holidays.add(LocalDate.parse(entry.path("date").asText()));
        }
        LocalDate candidate = from;
        while (candidate.getDayOfWeek() == DayOfWeek.SATURDAY || candidate.getDayOfWeek() == DayOfWeek.SUNDAY
                || holidays.contains(candidate)) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }

    private String loadHolidayCalendar() throws IOException, InterruptedException {
        return HTTP_CLIENT
                .send(HttpRequest.newBuilder(URI.create(calendarUrl)).GET().build(), HttpResponse.BodyHandlers.ofString())
                .body();
    }
}
