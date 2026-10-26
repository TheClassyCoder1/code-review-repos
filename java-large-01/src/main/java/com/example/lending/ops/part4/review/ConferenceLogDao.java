package com.example.lending.ops.part4.review;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Audit trail of who joined and left which review room, required by the credit committee policy. */
@Repository
@RequiredArgsConstructor
public class ConferenceLogDao {

	public static final class ConferenceLog {
		public enum Type { ROOM_ENTER, ROOM_LEAVE }
	}

	private final JdbcTemplate jdbcTemplate;

	public void add(ConferenceLog.Type type, Long userId, String streamId, Long roomId, String remoteAddress, String roomRef) {
		jdbcTemplate.update("INSERT INTO review_conference_log (event_type, user_id, stream_id, room_id, remote_address, room_ref)"
				+ " VALUES (?, ?, ?, ?, ?, ?)", type.name(), userId, streamId, roomId, remoteAddress, roomRef);
	}
}
