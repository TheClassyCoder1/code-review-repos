package com.example.lending.ops.part4.review;

import org.springframework.stereotype.Component;

/** Tears down a participant's screen-share streams when they leave a review room. */
@Component
public class MediaRoomHandler {

	public void leaveRoom(Client c) {
		c.getActiveStreams().clear();
	}
}
