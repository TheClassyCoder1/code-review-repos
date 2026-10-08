package com.example.lending.loan.archive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Extracts the searchable text of archived documents. */
@Component
public class DocumentIndexer {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentIndexer.class);
    private static final String[] SEARCHABLE_FILE_SUFFIXES = {".txt", ".csv", ".xml", ".json", ".html"};

    protected String getDocumentContent(final Path doc) {
        final String filename = doc.getFileName().toString();

        boolean searchSuffix = Arrays.stream(SEARCHABLE_FILE_SUFFIXES).anyMatch(filename::endsWith);

        String out = filename;
        if (searchSuffix) {
            try (final InputStream docStream = Files.newInputStream(doc); final StringWriter sout = new StringWriter()) {
                new InputStreamReader(docStream, StandardCharsets.UTF_8).transferTo(sout);
                out = out + " " + sout;
            } catch (final IOException e) {
                LOG.error("Archived document cannot be loaded", e);
            }
        }

        return out;
    }
}
