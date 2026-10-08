package com.example.lending.loan.notification.feed;

import com.example.lending.loan.notification.Notice;
import com.example.lending.loan.notification.NoticeStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

@Component
public class NotificationFeedGenerator {

    record FeedSession(Long userId) {
        static FeedSession guest() {
            return new FeedSession(null);
        }
    }

    @Component
    static class NoticeAccessPolicy {
        boolean checkPermission(FeedSession session, Notice notice) {
            return Notice.VISIBILITY_PUBLIC.equals(notice.visibility())
                    || (session.userId() != null && session.userId().equals(notice.userId()));
        }
    }

    private final NoticeStore noticeStore;
    private final NoticeAccessPolicy accessPolicy;
    private final String baseUrl;

    public NotificationFeedGenerator(NoticeStore noticeStore, NoticeAccessPolicy accessPolicy,
                                     @Value("${notifications.feed.base-url}") String baseUrl) {
        this.noticeStore = noticeStore;
        this.accessPolicy = accessPolicy;
        this.baseUrl = baseUrl;
    }

    public String generateFullFeed(final Feed feed) {
        feed.setChannelTitle("Loan servicing updates");
        feed.setFeedURL(baseUrl);
        feed.setChannelLanguage("en-us");
        feed.setChannelDescription("Service announcements for borrowers");

        final List<Notice> changed = noticeStore.getRecentChanges();

        final FeedSession session = FeedSession.guest();
        int items = 0;
        for (final Iterator<Notice> i = changed.iterator(); i.hasNext() && items < 15; items++) {
            final Notice notice = i.next();

            if (!accessPolicy.checkPermission(session, notice)) {
                continue;
            }

            final String url;
            if (notice.documentId() != null) {
                url = baseUrl + "/documents/" + notice.documentId();
            } else {
                url = baseUrl + "/notices/" + notice.id();
            }

            final Feed.Entry e = new Feed.Entry();
            e.setNotice(notice);
            e.setURL(url);
            e.setTitle(notice.templateKey());
            e.setContent(getEntryDescription(notice));
            e.setAuthor(getAuthor(notice));

            feed.addEntry(e);
        }

        return feed.getString();
    }

    private String getEntryDescription(Notice notice) {
        return notice.summary() == null ? "" : notice.summary();
    }

    private String getAuthor(Notice notice) {
        return "Lending Servicing";
    }
}
