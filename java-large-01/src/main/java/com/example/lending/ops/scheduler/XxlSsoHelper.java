package com.example.lending.ops.scheduler;

import com.example.lending.ops.common.Result.Response;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Value;
import java.lang.annotation.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Scheduler admin sessions, keyed by the {@code xxl-sso-session-id} request header. */
public final class XxlSsoHelper {

	@Retention(RetentionPolicy.RUNTIME)
	public @interface XxlSso { }

	@Value
	public static class LoginInfo { String userId, userName; }

	public static final class I18nUtil {
		public static String getString(String key) { return ResourceBundle.getBundle("i18n/scheduler").getString(key); }
	}

	public static final class Sha256Tool {
		public static String sha256(String input) {
			try {
				return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes()));
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
	}

	static final Map<String, LoginInfo> SESSIONS = new ConcurrentHashMap<>();

	public static Response<LoginInfo> loginCheckWithAttr(HttpServletRequest request) {
		LoginInfo info = SESSIONS.get(String.valueOf(request.getHeader("xxl-sso-session-id")));
		return info == null ? Response.ofFail("not logged in") : Response.success(info);
	}
}
