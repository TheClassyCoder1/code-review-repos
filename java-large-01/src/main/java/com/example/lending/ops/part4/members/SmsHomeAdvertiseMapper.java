package com.example.lending.ops.part4.members;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SmsHomeAdvertiseMapper {

    private final JdbcTemplate jdbcTemplate;

    public int insert(SmsHomeAdvertise row) {
        return jdbcTemplate.update("INSERT INTO sms_home_advertise (name, type, pic, start_time, end_time, status,"
                        + " click_count, order_count, url, note, sort) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                row.getName(), row.getType(), row.getPic(), row.getStartTime(), row.getEndTime(), row.getStatus(),
                row.getClickCount(), row.getOrderCount(), row.getUrl(), row.getNote(), row.getSort());
    }
}
