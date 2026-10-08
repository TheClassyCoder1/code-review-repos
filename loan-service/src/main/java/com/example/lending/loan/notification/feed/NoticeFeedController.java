package com.example.lending.loan.notification.feed;

import com.example.lending.loan.notification.NoticeStore;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class NoticeFeedController {

    private final NotificationFeedGenerator feedGenerator;
    private final NoticeStore noticeStore;

    public NoticeFeedController(NotificationFeedGenerator feedGenerator, NoticeStore noticeStore) {
        this.feedGenerator = feedGenerator;
        this.noticeStore = noticeStore;
    }

    @GetMapping(value = "/api/v1/notices/feed.xml", produces = "application/rss+xml")
    public String feed() {
        return feedGenerator.generateFullFeed(new Feed());
    }

    @PostMapping("/api/v1/ops/announcements")
    public Map<String, Long> announce(@RequestBody Map<String, String> request) {
        return Map.of("id", noticeStore.publishAnnouncement(request.get("key"), request.get("summary")));
    }
}
