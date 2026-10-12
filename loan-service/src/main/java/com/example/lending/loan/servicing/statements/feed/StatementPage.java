package com.example.lending.loan.servicing.statements.feed;

import java.time.Instant;

/** Published statement as listed in a feed. */
public class StatementPage {

    /** Statement whose content is a downloadable document rather than an HTML page. */
    public static class StatementAttachmentPage extends StatementPage {

        public StatementAttachmentPage(String name, int version, Instant lastModified, String title, String summary) {
            super(name, version, lastModified, title, summary);
        }
    }

    private final String name;
    private final int version;
    private final Instant lastModified;
    private final String title;
    private final String summary;

    public StatementPage(String name, int version, Instant lastModified, String title, String summary) {
        this.name = name;
        this.version = version;
        this.lastModified = lastModified;
        this.title = title;
        this.summary = summary;
    }

    public String getName() { return name; }
    public int getVersion() { return version; }

    public Instant getLastModified() { return lastModified; }
    public String getTitle() { return title; }

    public String getSummary() { return summary; }
}
