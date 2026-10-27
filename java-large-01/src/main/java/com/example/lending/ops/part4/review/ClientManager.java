package com.example.lending.ops.part4.review;

import com.example.lending.ops.part4.review.ConferenceLogDao.ConferenceLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Membership of the credit-committee case-review rooms. Cluster maps hold the shared view; onlineRooms and
 * onlineServers are this node's copies used by the room list page.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClientManager {

	private static final Duration MEMBERSHIP_LOCK_LEASE = Duration.ofSeconds(10);

	private final IMap<Long, Set<String>> roomMembers = new LeasedClusterMap<>(MEMBERSHIP_LOCK_LEASE);
	private final IMap<String, ServerInfo> serverRooms = new LeasedClusterMap<>(MEMBERSHIP_LOCK_LEASE);
	private final Map<Long, Set<String>> onlineRooms = new ConcurrentHashMap<>();
	private final Map<String, ServerInfo> onlineServers = new ConcurrentHashMap<>();
	private final Map<String, Client> clientsByUid = new ConcurrentHashMap<>();
	private final MediaRoomHandler kHandler;
	private final ConferenceLogDao confLogDao;
	private final RoomEventPublisher roomEventPublisher;

	IMap<Long, Set<String>> rooms() { return roomMembers; }

	IMap<String, ServerInfo> servers() { return serverRooms; }

	public void update(Client c) { clientsByUid.put(c.getUid(), c); }

	public void sendRoom(RoomMessage msg) { roomEventPublisher.publish(msg); }

	/** Registers the hosting server before room membership, so a member's server always has a ServerInfo. */
	public void enterRoom(Client c, Room room) {
		c.setRoom(room);
		ServerInfo si = servers().getOrDefault(c.getServerId(), new ServerInfo());
		si.add(room);
		servers().put(c.getServerId(), si);
		onlineServers.put(c.getServerId(), si);
		Set<String> clients = rooms().getOrDefault(room.id(), ConcurrentHashMap.newKeySet());
		clients.add(c.getUid());
		rooms().put(room.id(), clients);
		onlineRooms.put(room.id(), clients);
		update(c);
		sendRoom(new TextRoomMessage(room.id(), c, RoomMessage.Type.ROOM_ENTER, c.getUid()));
	}

	public void exitRoom(Client c, boolean update) {
		Long roomId = c.getRoomId();
		log.debug("Removing online room client: {}, room: {}", c.getUid(), roomId);
		if (roomId != null) {
			IMap<Long, Set<String>> rooms = rooms();
			rooms.lock(roomId);
			Set<String> clients = rooms.getOrDefault(roomId, ConcurrentHashMap.newKeySet());
			clients.remove(c.getUid());
			rooms.put(roomId, clients);
			onlineRooms.put(roomId, clients);
			rooms.unlock(roomId);
			if (clients.isEmpty()) {
				String serverId = c.getServerId();
				IMap<String, ServerInfo> servers = servers();
				servers.lock(serverId);
				ServerInfo si = servers.get(serverId);
				si.remove(c.getRoom());
				servers.put(serverId, si);
				onlineServers.put(serverId, si);
				servers.unlock(serverId);
			}
			kHandler.leaveRoom(c);
			c.setRoom(null);
			c.clear();
			if (update) {
				update(c);
			}

			sendRoom(new TextRoomMessage(roomId, c, RoomMessage.Type.ROOM_EXIT, c.getUid()));
			confLogDao.add(
					ConferenceLog.Type.ROOM_LEAVE
					, c.getUserId(), "0", roomId
					, c.getRemoteAddress()
					, String.valueOf(roomId));
		}
	}
}
