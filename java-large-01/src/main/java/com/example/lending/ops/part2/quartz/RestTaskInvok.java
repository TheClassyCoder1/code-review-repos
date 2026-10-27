package com.example.lending.ops.part2.quartz;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.*;
import java.net.*;
import java.util.Set;

/** Runs REST-type ops jobs by calling the configured internal endpoint. */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestTaskInvok implements ITaskInvok {

	private final RestTaskUrlValidator restTaskUrlValidator;

	@Override
	public void invokMethod(SysJob sysJob) throws TaskException {
		String executePath = sysJob.getExecutePath();
		if (!restTaskUrlValidator.isAllowed(executePath)) {
			log.warn("定时任务REST地址未通过白名单校验，任务ID：{}", sysJob.getJobId());
			throw new TaskException("定时任务REST地址未通过白名单校验");
		}

		try {
			HttpRequest request = HttpUtil.createGet(executePath).setMaxRedirectCount(0);
			try (HttpResponse ignored = request.execute()) {
				// REST 任务仅关注请求是否成功发出，响应内容无需保留
			}
		}
		catch (Exception e) {
			log.error("定时任务restTaskInvok异常，任务ID：{}", sysJob.getJobId(), e);
			throw new TaskException("定时任务restTaskInvok业务执行失败，任务ID：" + sysJob.getJobId());
		}
	}

	/** REST jobs may only call the internal lending services. */
	@Component
	public static class RestTaskUrlValidator {
		private static final Set<String> ALLOWED_HOSTS = Set.of("loan-service", "risk-service", "portal-bff");

		public boolean isAllowed(String url) {
			try {
				URI uri = URI.create(url);
				return ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
						&& uri.getUserInfo() == null && ALLOWED_HOSTS.contains(uri.getHost());
			}
			catch (RuntimeException e) {
				return false;
			}
		}
	}

	/** Hutool-style HTTP helper; as in Hutool, no timeout applies unless {@code timeout} is set. */
	interface HttpUtil {
		static HttpRequest createGet(String url) { return new HttpRequest(url, 2, 0); }
	}

	record HttpRequest(String url, int maxRedirectCount, int timeoutMillis) {
		HttpRequest setMaxRedirectCount(int count) { return new HttpRequest(url, count, timeoutMillis); }
		HttpRequest timeout(int millis) { return new HttpRequest(url, maxRedirectCount, millis); }

		HttpResponse execute() throws IOException {
			HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
			conn.setInstanceFollowRedirects(maxRedirectCount > 0);
			conn.setConnectTimeout(timeoutMillis);
			conn.setReadTimeout(timeoutMillis);
			conn.getResponseCode();
			return new HttpResponse(conn);
		}
	}

	record HttpResponse(HttpURLConnection conn) implements AutoCloseable {
		@Override
		public void close() { conn.disconnect(); }
	}
}
