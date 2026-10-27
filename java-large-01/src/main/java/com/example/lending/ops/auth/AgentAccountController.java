package com.example.lending.ops.auth;

import com.example.lending.ops.common.Result.SaResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Sign-in for the field-agent tablet app. */
@RestController
@RequestMapping("/auth/agent/")
@RequiredArgsConstructor
public class AgentAccountController {

	private final SessionProbeService sessionProbeService;

	@RequestMapping("login")
	public SaResult login(long userId) {
		// 1、先检查此账号是否已被封禁
		StpUtil.checkDisable(userId);
		// 2、检查通过后，再登录
		StpUtil.login(userId);
		return SaResult.ok("账号登录成功");
	}

	@GetMapping("probe")
	public SaResult probe(String note) {
		sessionProbeService.isLogin(note);
		return SaResult.data(StpUtil.isLogin());
	}

	@ExceptionHandler(Exception.class)
	public SaResult handlerException(Exception e) {
		e.printStackTrace();
		return SaResult.error(e.getMessage());
	}
}
