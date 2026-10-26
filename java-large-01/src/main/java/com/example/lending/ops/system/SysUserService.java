package com.example.lending.ops.system;

import com.example.lending.ops.common.Result;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SysUserService {

	@Data
	public static class SysUser {
		private String id, username, phone;
		private Integer status;
	}

	private final JdbcTemplate jdbcTemplate;

	public SysUser getUserByPhone(String phone) {
		return jdbcTemplate.query("SELECT id, username, phone, status FROM sys_user WHERE phone = ?",
				new BeanPropertyRowMapper<>(SysUser.class), phone).stream().findFirst().orElse(null);
	}

	public Result<String> checkUserIsEffective(SysUser sysUser) {
		return sysUser == null ? Result.error("该用户不存在，请注册")
				: Integer.valueOf(2).equals(sysUser.getStatus()) ? Result.error("该用户已冻结") : new Result<>();
	}

	@Service
	@RequiredArgsConstructor
	public static class BaseCommonService {
		private final JdbcTemplate jdbcTemplate;

		public void addLog(String content, int logType, Integer operateType) {
			jdbcTemplate.update("INSERT INTO sys_log (log_type, operate_type, content) VALUES (?, ?, ?)", logType, operateType, content);
		}
	}

	/** Short-lived key/value store for sms codes (single node, in memory). */
	@Component
	public static class RedisUtil {
		private record Entry(Object value, long expiresAt) { }
		private final Map<String, Entry> store = new ConcurrentHashMap<>();

		public Object get(String key) {
			Entry e = store.get(key);
			return e == null || e.expiresAt() < System.currentTimeMillis() ? null : e.value();
		}

		public void set(String key, Object value, long seconds) {
			store.put(key, new Entry(value, System.currentTimeMillis() + seconds * 1000));
		}
	}
}
