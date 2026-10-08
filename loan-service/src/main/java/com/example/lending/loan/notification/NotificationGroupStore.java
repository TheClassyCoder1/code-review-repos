package com.example.lending.loan.notification;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationGroupStore {

    private final JdbcTemplate jdbc;

    public NotificationGroupStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean insert(String id, String groupId, String groupName, Long memberId, String memberName,
                          String memberType, Long createdBy) {
        return jdbc.update("INSERT INTO lending.notification_group_members (id, group_id, group_name, member_id, "
                + "member_name, member_type, created_by, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, now())",
                id, groupId, groupName, memberId, memberName, memberType, createdBy) == 1;
    }
}
