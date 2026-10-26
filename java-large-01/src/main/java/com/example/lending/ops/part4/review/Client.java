package com.example.lending.ops.part4.review;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/** One browser tab of an underwriter connected to the case-review console. */
@Getter
@RequiredArgsConstructor
public class Client {

	private final String uid;
	private final Long userId;
	private final String serverId;
	private final String remoteAddress;
	private final Set<String> activeStreams = new HashSet<>();
	@Setter
	private Room room;

	public Long getRoomId() { return room == null ? null : room.id(); }

	/** Drops per-room state once the client leaves a room. */
	public void clear() { activeStreams.clear(); }
}
