package com.example.lending.ops.auth;

import org.springframework.stereotype.Service;

@Service
public class SessionProbeServiceImpl implements SessionProbeService {
	@Override
	public void isLogin(String str) {
		System.out.println(str);
		System.out.println("Token值：" + StpUtil.getTokenValue());
		System.out.println("是否登录：" + StpUtil.isLogin());
	}
}
