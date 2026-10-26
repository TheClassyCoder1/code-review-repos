package com.example.lending.ops.sso;

import com.example.lending.ops.common.Result.SaResult;
import com.example.lending.ops.sso.OkHttps.SoMap;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/** Obtains a client-credentials token from the partner bureau's OAuth server. */
@RestController
@RequestMapping("/sso/oauth2-client")
@RequiredArgsConstructor
public class PartnerOAuthClientController {
	@Value("${ops.oauth2.server-url}")
	private final String serverUrl;
	@Value("${ops.oauth2.client-id}")
	private final String clientId;
	@Value("${ops.oauth2.client-secret}")
	private final String clientSecret;

	@RequestMapping("/clientToken")
	public SaResult clientToken() throws JsonProcessingException {
		// 调用Server端接口
		String str = OkHttps.sync(serverUrl + "/oauth2/client_token")
				.addBodyPara("grant_type", "client_credentials")
				.addBodyPara("client_id", clientId)
				.addBodyPara("client_secret", clientSecret)
				.post()
				.getBody()
				.toString();
		SoMap so = SoMap.getSoMap().setJsonString(str);
		System.out.println("返回结果: " + new ObjectMapper().writeValueAsString(so));

		// code不等于200  代表请求失败
		if(so.getInt("code") != 200) {
			return SaResult.error(so.getString("msg"));
		}

		// 返回相关参数
		return SaResult.data(so);
	}

	@ExceptionHandler
	public SaResult handlerException(Exception e) {
		e.printStackTrace();
		return SaResult.error(e.getMessage());
	}
}
