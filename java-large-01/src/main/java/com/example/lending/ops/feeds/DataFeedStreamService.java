package com.example.lending.ops.feeds;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DataFeedStreamService {

    /** A partner data feed (repayment files, bureau pulls) registered under a feed group. */
    @Data
    public static class InlongStreamInfo { private String groupId, streamId, tenantId, owners, sinkLocation; }

    public enum ErrorCodeEnum { INVALID_PARAMETER, LOGIN_USER_EMPTY }

    public static final class Preconditions {
        public static void expectNotBlank(String value, ErrorCodeEnum errorCode, String message) {
            if (value == null || value.isBlank()) throw new IllegalArgumentException(errorCode + ": " + message);
        }

        public static void expectNotNull(Object value, ErrorCodeEnum errorCode) {
            expectNotBlank(value == null ? null : "ok", errorCode, errorCode.name());
        }
    }

    private final JdbcTemplate jdbcTemplate;

    public InlongStreamInfo get(String groupId, String streamId) {
        return jdbcTemplate.queryForObject("SELECT * FROM feed_stream WHERE group_id = ? AND stream_id = ?",
                new BeanPropertyRowMapper<>(InlongStreamInfo.class), groupId, streamId);
    }
}
