package com.example.lending.loan.notification.feed;

import com.example.lending.loan.notification.Notice;

import java.util.ArrayList;
import java.util.List;

/** Minimal RSS 2.0 writer. */
public class Feed {

    public static class Entry {
        private Notice notice;
        private String url;
        private String title;
        private String content;
        private String author;

        public void setNotice(Notice notice) { this.notice = notice; }
        public void setURL(String url) { this.url = url; }
        public void setTitle(String title) { this.title = title; }
        public void setContent(String content) { this.content = content; }
        public void setAuthor(String author) { this.author = author; }
    }

    private String channelTitle;
    private String feedURL;
    private String channelLanguage;
    private String channelDescription;
    private final List<Entry> entries = new ArrayList<>();

    public void setChannelTitle(String channelTitle) { this.channelTitle = channelTitle; }
    public void setFeedURL(String feedURL) { this.feedURL = feedURL; }
    public void setChannelLanguage(String channelLanguage) { this.channelLanguage = channelLanguage; }
    public void setChannelDescription(String channelDescription) { this.channelDescription = channelDescription; }

    public void addEntry(Entry entry) {
        entries.add(entry);
    }

    public String getString() {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><rss version=\"2.0\"><channel>");
        element(sb, "title", channelTitle);
        element(sb, "link", feedURL);
        element(sb, "language", channelLanguage);
        element(sb, "description", channelDescription);
        for (Entry entry : entries) {
            sb.append("<item>");
            element(sb, "title", entry.title);
            element(sb, "link", entry.url);
            element(sb, "description", entry.content);
            element(sb, "author", entry.author);
            element(sb, "guid", entry.notice == null ? null : String.valueOf(entry.notice.id()));
            sb.append("</item>");
        }
        return sb.append("</channel></rss>").toString();
    }

    private static void element(StringBuilder sb, String name, String value) {
        if (value != null) {
            sb.append('<').append(name).append('>')
                    .append(value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"))
                    .append("</").append(name).append('>');
        }
    }
}
