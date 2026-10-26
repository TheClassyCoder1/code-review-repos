package com.example.lending.ops.part3.web;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Caches the body so the partner signature check and the controller can both read it. */
@Slf4j
public class RepeatBodyRequestWrapper extends HttpServletRequestWrapper {
	private final byte[] bodyByteArray;
	public RepeatBodyRequestWrapper(HttpServletRequest request) {
		super(request);
		this.bodyByteArray = getByteBody(request);
	}
	public byte[] getBodyBytes() { return bodyByteArray; }
	@Override
	public BufferedReader getReader() { return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8)); }
	@Override
	public ServletInputStream getInputStream() {
		ByteArrayInputStream bais = new ByteArrayInputStream(bodyByteArray);
		return new ServletInputStream() {
			public boolean isFinished() { return bais.available() == 0; }
			public boolean isReady() { return true; }
			public void setReadListener(ReadListener readListener) { }
			public int read() { return bais.read(); }
		};
	}
	private static byte[] getByteBody(HttpServletRequest request) {
		byte[] body = new byte[0];
		try {
			body = StreamUtils.copyToByteArray(request.getInputStream());
		}
		catch (IOException e) {
			log.error("解析流中数据异常", e);
		}
		return body;
	}
}
