package com.example.lending.loan.servicing.statements.feed;

import com.example.lending.loan.servicing.statements.feed.Feed.FeedEntry;
import com.example.lending.loan.servicing.statements.feed.StatementPage.StatementAttachmentPage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/** Builds the RSS feed of recently published statements shown in the borrower portal. */
@Component
public class StatementFeedGenerator {

    static final String PROP_CHANNEL_LANGUAGE = "feed.channelLanguage";
    static final String PROP_CHANNEL_DESCRIPTION = "feed.channelDescription";
    static final String CONTEXT_VIEW = "statements";
    static final String CONTEXT_ATTACH = "documents";

    /** Newest first, then by name. */
    static class PageTimeComparator implements Comparator<StatementPage> {

        @Override
        public int compare(StatementPage a, StatementPage b) {
            int byTime = b.getLastModified().compareTo(a.getLastModified());
            return byTime != 0 ? byTime : a.getName().compareTo(b.getName());
        }
    }

    private final String applicationName;
    private final String channelLanguage;

    public StatementFeedGenerator(@Value("${servicing.portal.application-name:Loan Servicing}") String applicationName,
                                  @Value("${servicing.portal.feed-language:en-us}") String channelLanguage) {
        this.applicationName = applicationName;
        this.channelLanguage = channelLanguage;
    }

    public String generateStatementFeed(final StatementFeedContext feedContext, final List<StatementPage> changed, final Feed feed) {
        feed.setChannelTitle(applicationName + ": " + feedContext.getFeedName());
        feed.setFeedURL(feedContext.getViewURL(feedContext.getFeedName()));
        final String language = feedContext.getVariable(PROP_CHANNEL_LANGUAGE);

        if (language != null) {
            feed.setChannelLanguage(language);
        } else {
            feed.setChannelLanguage(channelLanguage);
        }
        final String channelDescription = feedContext.getVariable(PROP_CHANNEL_DESCRIPTION);

        if (channelDescription != null) {
            feed.setChannelDescription(channelDescription);
        }

        changed.sort(new PageTimeComparator());

        int items = 0;
        for (final Iterator<StatementPage> i = changed.iterator(); i.hasNext() && items < 15; items++) {
            final StatementPage page = i.next();
            final FeedEntry e = new FeedEntry();
            e.setPage(page);
            String url;

            if (page instanceof StatementAttachmentPage) {
                url = feedContext.getURL(CONTEXT_ATTACH, page.getName(), "version=" + page.getVersion());
            } else {
                url = feedContext.getURL(CONTEXT_VIEW, page.getName(), "version=" + page.getVersion());
            }

            // The feed writer escapes the URL again on output
            url = url.replace("&amp;", "&");
            e.setURL(url);
            e.setTitle(page.getTitle());
            e.setContent(page.getSummary());
            e.setAuthor(applicationName);

            feed.addEntry(e);
        }

        return feed.getString();
    }
}
