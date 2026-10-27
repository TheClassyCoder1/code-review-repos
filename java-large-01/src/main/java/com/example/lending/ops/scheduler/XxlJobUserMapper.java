package com.example.lending.ops.scheduler;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class XxlJobUserMapper {

	/** Account for the batch-job scheduler admin (nightly accruals, dunning runs). */
	@Data
	public static class XxlJobUser {
		private int id;
		private String username, password;
	}

	private final JdbcTemplate jdbcTemplate;

	public XxlJobUser loadByUserName(String username) {
		return jdbcTemplate.queryForObject("SELECT id, username, password FROM xxl_job_user WHERE username = ?",
				new BeanPropertyRowMapper<>(XxlJobUser.class), username);
	}

	public int update(XxlJobUser user) {
		return jdbcTemplate.update("UPDATE xxl_job_user SET password = ? WHERE id = ?", user.getPassword(), user.getId());
	}
}
