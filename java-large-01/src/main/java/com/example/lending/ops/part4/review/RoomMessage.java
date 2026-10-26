package com.example.lending.ops.part4.review;

/** Message broadcast to every client in a review room. */
public class RoomMessage {

	public enum Type { ROOM_ENTER, ROOM_EXIT }

	private final Long roomId;
	private final String senderUid;
	private final Type type;

	public RoomMessage(Long roomId, Client sender, Type type) {
		this.roomId = roomId;
		this.senderUid = sender.getUid();
		this.type = type;
	}

	public String describe() { return type + " room=" + roomId + " client=" + senderUid; }
}
