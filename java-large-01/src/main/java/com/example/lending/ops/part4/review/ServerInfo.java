package com.example.lending.ops.part4.review;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Rooms currently hosted by one console node. */
public class ServerInfo {

	private final Set<Long> roomIds = ConcurrentHashMap.newKeySet();

	public void add(Room room) { roomIds.add(room.id()); }

	public void remove(Room room) { roomIds.remove(room.id()); }
}
