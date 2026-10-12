package com.example.lending.loan.servicing.statements.feed;

import org.springframework.web.util.HtmlUtils;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** RSS 2.0 document. All text is XML-escaped on output. */
public class Feed {

    /** Item of an RSS feed. */
    public static class FeedEntry {

        private StatementPage page;
        private String url;
        private String title;
        private String content;
        private String author;

        public StatementPage getPage() { return page; }
        public void setPage(StatementPage page) { this.page = page; }

        public String getURL() { return url; }
        public void setURL(String url) { this.url = url; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
    }

    private String channelTitle;
    private String channelDescription;
    private String channelLanguage;
    private String feedURL;
    private final List<FeedEntry> entries = new ArrayList<>();

    public void setChannelTitle(String channelTitle) { this.channelTitle = channelTitle; }
    public void setChannelDescription(String channelDescription) { this.channelDescription = channelDescription; }

    public void setChannelLanguage(String channelLanguage) { this.channelLanguage = channelLanguage; }
    public void setFeedURL(String feedURL) { this.feedURL = feedURL; }

    public void addEntry(FeedEntry entry) { entries.add(entry); }

    public String getString() {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<rss version=\"2.0\"><channel>");
        element(xml, "title", channelTitle);
        element(xml, "link", feedURL);
        element(xml, "description", channelDescription == null ? "" : channelDescription);
        element(xml, "language", channelLanguage);
        for (FeedEntry entry : entries) {
            xml.append("<item>");
            element(xml, "title", entry.getTitle());
            element(xml, "link", entry.getURL());
            element(xml, "description", entry.getContent());
            element(xml, "author", entry.getAuthor());
            element(xml, "pubDate", DateTimeFormatter.RFC_1123_DATE_TIME
                    .format(entry.getPage().getLastModified().atOffset(ZoneOffset.UTC)));
            xml.append("</item>");
        }
        return xml.append("</channel></rss>").toString();
    }

    private static void element(StringBuilder xml, String name, String value) {
        if (value != null) {
            xml.append('<').append(name).append('>').append(HtmlUtils.htmlEscape(value)).append("</").append(name).append('>');
        }
    }
}
