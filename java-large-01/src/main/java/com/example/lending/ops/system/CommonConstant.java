package com.example.lending.ops.system;

import jakarta.servlet.http.HttpServletRequest;

public interface CommonConstant {
	String PHONE_REDIS_KEY_PRE = "phone_msg:";
	Integer PHONE_SMS_FAIL_CODE = 40002;
	/** sms templates: 0 login, 1 register, 2 forgotten password */
	String SMS_TPL_TYPE_0 = "0", SMS_TPL_TYPE_1 = "1", SMS_TPL_TYPE_2 = "2";
	int LOG_TYPE_1 = 1;

	final class IpUtils {
		/** The edge proxy overwrites X-Forwarded-For with the real client address. */
		public static String getIpAddr(HttpServletRequest request) {
			String forwarded = request.getHeader("X-Forwarded-For");
			return forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
		}
	}
}
