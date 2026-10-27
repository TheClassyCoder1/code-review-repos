package com.example.lending.ops.sso;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.util.*;

/** Minimal synchronous form-post client for the partner OAuth server. */
public final class OkHttps {

	private static final RestTemplate REST = new RestTemplate();

	public static SyncTask sync(String url) { return new SyncTask(url, new LinkedMultiValueMap<>()); }

	public record SyncTask(String url, LinkedMultiValueMap<String, String> form) {
		public SyncTask addBodyPara(String name, String value) {
			form.add(name, value);
			return this;
		}

		public ResponseEntity<String> post() { return REST.postForEntity(url, form, String.class); }
	}

	public static class SoMap extends LinkedHashMap<String, Object> {
		public static SoMap getSoMap() { return new SoMap(); }
		public int getInt(String key) { return Integer.parseInt(String.valueOf(get(key))); }
		public String getString(String key) { return String.valueOf(get(key)); }

		@SuppressWarnings("unchecked")
		public SoMap setJsonString(String json) {
			try {
				putAll(new ObjectMapper().readValue(json, Map.class));
				return this;
			} catch (Exception e) {
				throw new IllegalArgumentException("not a json object", e);
			}
		}
	}
}
