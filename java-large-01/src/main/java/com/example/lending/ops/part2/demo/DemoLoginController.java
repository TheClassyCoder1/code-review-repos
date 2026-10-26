package com.example.lending.ops.part2.demo;

import com.example.lending.ops.auth.StpUtil;
import com.example.lending.ops.common.Result.SaResult;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.*;

/** Walkthrough login for laptop demos of the agent tablet app; see {@link DemoModeCondition}. */
@RestController
@Conditional(DemoModeCondition.class)
@RequestMapping("/auth/demo/")
public class DemoLoginController {

	@RequestMapping("doLogin")
	public SaResult doLogin(String name, String pwd) {

		// 第一步：比对前端提交的 账号名称 & 密码 是否正确，比对成功后开始登录
		// 		此处仅作模拟示例，真实项目需要从数据库中查询数据进行比对
		if("zhang".equals(name) && "123456".equals(pwd)) {

			// 第二步：根据账号id，进行登录
			// 		此处填入的参数应该保持用户表唯一，比如用户id，不可以直接填入整个 User 对象
			StpUtil.login(10001);

			// SaResult 是 Sa-Token 中对返回结果的简单封装，下面的示例将不再赘述
			return SaResult.ok("登录成功");
		}

		return SaResult.error("登录失败");
	}
}
