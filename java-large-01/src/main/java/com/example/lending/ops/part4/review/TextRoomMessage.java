package com.example.lending.ops.part4.review;

public class TextRoomMessage extends RoomMessage {

	private final String text;

	public TextRoomMessage(Long roomId, Client sender, Type type, String text) {
		super(roomId, sender, type);
		this.text = text;
	}

	public String getText() {
		return text;
	}
}
