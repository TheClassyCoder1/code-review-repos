package com.example.lending.ops.system;

import com.example.lending.ops.common.JSONObject;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.concurrent.*;

/** Hands sms messages to the carrier relay. */
public final class DySmsHelper {

	public enum DySmsEnum { LOGIN_TEMPLATE_CODE, REGISTER_TEMPLATE_CODE, FORGET_PASSWORD_TEMPLATE_CODE }

	public static class ClientException extends Exception {
		ClientException(Throwable cause) { super("sms relay rejected the request", cause); }
	}

	/** At most 5 sms per client IP per minute. */
	public static class DySmsLimit {
		private static final Map<String, Deque<Long>> SENT = new ConcurrentHashMap<>();

		public static synchronized boolean canSendSms(String ip) {
			Deque<Long> sent = SENT.computeIfAbsent(ip, k -> new ArrayDeque<>());
			sent.removeIf(t -> t < System.currentTimeMillis() - 60_000);
			return sent.size() < 5 && sent.add(System.currentTimeMillis());
		}
	}

	public static class RandomUtil {
		public static String randomNumbers(int n) { return String.format("%0" + n + "d", ThreadLocalRandom.current().nextLong((long) Math.pow(10, n))); }
	}

	private static final RestTemplate REST = new RestTemplate();
	private static final String RELAY_URL = System.getenv().getOrDefault("OPS_SMS_RELAY_URL", "http://localhost:8120/sms/send");

	public static boolean sendSms(String phone, JSONObject templateParam, DySmsEnum template) throws ClientException {
		try {
			REST.postForEntity(RELAY_URL, Map.of("phone", phone, "template", template.name(), "params", templateParam), Void.class);
			return true;
		} catch (RuntimeException e) {
			throw new ClientException(e);
		}
	}
}
