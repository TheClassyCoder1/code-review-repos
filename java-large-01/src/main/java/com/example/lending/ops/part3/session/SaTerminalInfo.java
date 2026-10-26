package com.example.lending.ops.part3.session;

import lombok.*;

/** One device an agent is signed in from; one entry per issued console token. */
@Data
@AllArgsConstructor
public class SaTerminalInfo {
	private int index;
	private String tokenValue, deviceType;
	private long createTime;
}
