package com.example.lending.loan.service;

import com.example.lending.loan.dto.StatementImportSummary;
import com.example.lending.loan.dto.StatementMetadata;
import com.example.lending.loan.util.StreamBytes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Service
public class StatementImportService {

    private static final String CSV = "text/csv";

    public StatementImportSummary importStatement(String contentType, InputStream input) throws IOException {
        if (!CSV.equals(normalizeContentType(contentType))) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
        byte[] bytes = StreamBytes.toByteArray(input);
        List<String> lines = new String(bytes, StandardCharsets.UTF_8).lines()
                .filter(line -> !line.isBlank())
                .toList();
        StatementMetadata metadata = StatementMetadata.parse(lines.isEmpty() ? null : lines.get(0));
        int skipped = metadata.headers().isEmpty() ? 1 : 2;
        String currency = metadata.headers().containsKey("currency") ? metadata.getHeadersOrThrow("currency") : null;
        return new StatementImportSummary(Math.max(lines.size() - skipped, 0), bytes.length, currency);
    }

    private static String normalizeContentType(String value) {
        String type = normalize(value);
        int parameter = type.indexOf(';');
        if (parameter >= 0) {
            type = type.substring(0, parameter).trim();
        }
        return type.toLowerCase(Locale.ENGLISH);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
