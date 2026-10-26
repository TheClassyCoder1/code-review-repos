package com.example.lending.ops.system;

import com.example.lending.ops.common.JSONObject;
import com.example.lending.ops.common.Result;
import com.example.lending.ops.common.StringUtils.oConvertUtils;
import com.example.lending.ops.system.CommonConstant.IpUtils;
import com.example.lending.ops.system.DySmsHelper.*;
import com.example.lending.ops.system.SysUserService.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/** Phone-number sign-in for branch staff without a console password. */
@Slf4j
@RestController
@RequestMapping("/sys")
@RequiredArgsConstructor
public class SmsLoginController {
	private final RedisUtil redisUtil;
	private final SysUserService sysUserService;
	private final BaseCommonService baseCommonService;

	@PostMapping(value = "/sms")
	public Result<String> sms(@RequestBody JSONObject jsonObject,HttpServletRequest request) {
		Result<String> result = new Result<String>();
		String clientIp = IpUtils.getIpAddr(request);
		String mobile = jsonObject.get("mobile").toString();
		//手机号模式 登录模式: "2"  注册模式: "1"
		String smsmode=jsonObject.get("smsmode").toString();
		log.info("-------- IP:{}, 手机号：{}，获取绑定验证码", clientIp, mobile);

		if(oConvertUtils.isEmpty(mobile)){
			result.setMessage("手机号不允许为空！");
			result.setSuccess(false);
			return result;
		}

		// VUEN-2245【漏洞】发现新漏洞待处理20220906
		String redisKey = CommonConstant.PHONE_REDIS_KEY_PRE+mobile;
		Object object = redisUtil.get(redisKey);

		if (object != null) {
			result.setMessage("验证码10分钟内，仍然有效！");
			result.setSuccess(false);
			return result;
		}

		//-------------------------------------------------------------------------------------
		//增加 check防止恶意刷短信接口
		if(!DySmsLimit.canSendSms(clientIp)){
			log.warn("--------[警告] IP地址:{}, 短信接口请求太多-------", clientIp);
			result.setMessage("短信接口请求太多，请稍后再试！");
			result.setCode(CommonConstant.PHONE_SMS_FAIL_CODE);
			result.setSuccess(false);
			return result;
		}
		//-------------------------------------------------------------------------------------

		//随机数
		String captcha = RandomUtil.randomNumbers(6);
		JSONObject obj = new JSONObject();
    	obj.put("code", captcha);
		try {
			boolean b = false;
			//注册模板
			if (CommonConstant.SMS_TPL_TYPE_1.equals(smsmode)) {
				SysUser sysUser = sysUserService.getUserByPhone(mobile);
				if(sysUser!=null) {
					result.error500(" 手机号已经注册，请直接登录！");
					baseCommonService.addLog("手机号已经注册，请直接登录！", CommonConstant.LOG_TYPE_1, null);
					return result;
				}
				b = DySmsHelper.sendSms(mobile, obj, DySmsEnum.REGISTER_TEMPLATE_CODE);
			}else {
				//登录模式，校验用户有效性
				SysUser sysUser = sysUserService.getUserByPhone(mobile);
				result = sysUserService.checkUserIsEffective(sysUser);
				if(!result.isSuccess()) {
					String message = result.getMessage();
					String userNotExist="该用户不存在，请注册";
					if(userNotExist.equals(message)){
						result.error500("该用户不存在或未绑定手机号");
					}
					return result;
				}

				/**
				 * smsmode 短信模板方式  0 .登录模板、1.注册模板、2.忘记密码模板
				 */
				if (CommonConstant.SMS_TPL_TYPE_0.equals(smsmode)) {
					//登录模板
					b = DySmsHelper.sendSms(mobile, obj, DySmsEnum.LOGIN_TEMPLATE_CODE);
				} else if(CommonConstant.SMS_TPL_TYPE_2.equals(smsmode)) {
					//忘记密码模板
					b = DySmsHelper.sendSms(mobile, obj, DySmsEnum.FORGET_PASSWORD_TEMPLATE_CODE);
                    // 代码逻辑说明: 【issues/8567】严重：修改密码存在水平越权问题。---
                    if(b){
                        String username = sysUser.getUsername();
                        obj.put("username",username);
                        redisUtil.set(redisKey, obj.toJSONString(), 600);
                        result.setSuccess(true);
                        return result;
                    }
                }
			}

			if (b == false) {
				result.setMessage("短信验证码发送失败,请稍后重试");
				result.setSuccess(false);
				return result;
			}

			//验证码10分钟内有效
			redisUtil.set(redisKey, captcha, 600);
			result.setSuccess(true);

		} catch (ClientException e) {
			e.printStackTrace();
			result.error500(" 短信接口未配置，请联系管理员！");
			return result;
		}
		return result;
	}
}
