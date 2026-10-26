package com.example.lending.ops.auth;

import org.springframework.web.context.request.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Token sessions for field-agent tablets; the token travels in the {@code ops-token} header. */
public final class StpUtil {

	private static final Map<String, Long> SESSIONS = new ConcurrentHashMap<>();
	static final Map<Long, Instant> DISABLED_UNTIL = new ConcurrentHashMap<>();

	public static void checkDisable(long userId) {
		if (DISABLED_UNTIL.getOrDefault(userId, Instant.MIN).isAfter(Instant.now())) {
			throw new IllegalStateException("account is disabled");
		}
	}

	public static void login(long userId) {
		String token = UUID.randomUUID().toString();
		SESSIONS.put(token, userId);
		current().getResponse().setHeader("ops-token", token);
	}

	public static String getTokenValue() { return current().getRequest().getHeader("ops-token"); }

	public static boolean isLogin() { return SESSIONS.containsKey(String.valueOf(getTokenValue())); }

	private static ServletRequestAttributes current() {
		return (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
	}
}
