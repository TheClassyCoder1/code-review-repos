package com.example.lending.ops.scheduler;

import com.example.lending.ops.common.Result.Response;
import com.example.lending.ops.scheduler.XxlJobUserMapper.XxlJobUser;
import com.example.lending.ops.scheduler.XxlSsoHelper.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/scheduler/auth")
@RequiredArgsConstructor
public class SchedulerLoginController {

	private final XxlJobUserMapper xxlJobUserMapper;

	@RequestMapping("/updatePwd")
	@ResponseBody
	@XxlSso
	public Response<String> updatePwd(HttpServletRequest request, String oldPassword, String password){

		// valid
		if (oldPassword==null || oldPassword.trim().isEmpty()){
			return Response.ofFail(I18nUtil.getString("system_please_input") + I18nUtil.getString("change_pwd_field_oldpwd"));
		}
		if (password==null || password.trim().isEmpty()){
			return Response.ofFail(I18nUtil.getString("system_please_input") + I18nUtil.getString("change_pwd_field_oldpwd"));
		}
		password = password.trim();
		if (!(password.length()>=4 && password.length()<=20)) {
			return Response.ofFail(I18nUtil.getString("system_length_limit")+"[4-20]" );
		}

		// md5 password
		String oldPasswordHash = Sha256Tool.sha256(oldPassword);
		String passwordHash = Sha256Tool.sha256(password);

		// valid old pwd
		Response<LoginInfo> loginInfoResponse = XxlSsoHelper.loginCheckWithAttr(request);
		XxlJobUser existUser = xxlJobUserMapper.loadByUserName(loginInfoResponse.getData().getUserName());
		if (!oldPasswordHash.equals(existUser.getPassword())) {
			return Response.ofFail(I18nUtil.getString("change_pwd_field_oldpwd") + I18nUtil.getString("system_invalid"));
		}

		// write new
		existUser.setPassword(passwordHash);
		xxlJobUserMapper.update(existUser);

		return Response.ofSuccess();
	}
}
