package com.example.lending.ops.part3.webhook;

import com.example.lending.ops.part3.support.StrUtil;
import com.example.lending.ops.part3.support.StrUtil.CollUtil;
import com.example.lending.ops.part3.webhook.OaFactory.OaSender;
import com.example.lending.ops.part3.webhook.WebhookConfigService.SysSystemConfigMapper;
import com.example.lending.ops.part3.webhook.WebhookModel.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import java.util.*;

/** Sends ops notifications to the chat-robot channel configured for a business code. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMessageServiceImpl implements SysMessageService {
	private final SysSystemConfigMapper sysSystemConfigMapper;
	private final RemoteLogService remoteLogService;
	@Override
	public R sendHook(MessageHookDTO messageHookDTO) {

		// 根据业务编码获取短信通道配置
		List<SysSystemConfigEntity> configEntityList = sysSystemConfigMapper
			.selectList(Wrappers.<SysSystemConfigEntity>lambdaQuery()
				.eq(SysSystemConfigEntity::getConfigType, SystemConfigTypeEnum.WEBHOOK.getValue())
				.eq(SysSystemConfigEntity::getConfigStatus, YesNoEnum.YES.getCode())
				.eq(SysSystemConfigEntity::getConfigKey, messageHookDTO.getBizCode()));
		if (CollUtil.isEmpty(configEntityList)) {
			return R.failed(MsgUtils.getMessage(UpmsErrorCodes.SYS_MESSAGE_SMS_CHANNEL_MISSING));
		}

		SysSystemConfigEntity configEntity = configEntityList.get(0);
		JSONObject configMap = JSONUtil.parseObj(configEntity.getConfigValue());
		String supplier = configMap.getStr("supplier");

		// 根据配置创建服务实例并注册
		OaSender alarm = OaFactory.getSmsOaBlend(messageHookDTO.getBizCode());
		if (Objects.isNull(alarm)) {
			if (StrUtil.equalsAnyIgnoreCase(OaType.DING_TALK.toString(), supplier)) {
				DingTalkConfig dingTalkConfig = new DingTalkConfig();
				dingTalkConfig.setConfigId(messageHookDTO.getBizCode());
				dingTalkConfig.setSign(configMap.getStr("sign"));
				dingTalkConfig.setTokenId(configMap.getStr("tokenId"));
				OaFactory.createAndRegisterOaSender(dingTalkConfig);
			}
			if (StrUtil.equalsAnyIgnoreCase(OaType.BYTE_TALK.toString(), supplier)) {
				ByteTalkConfig byteTalkConfig = new ByteTalkConfig();
				byteTalkConfig.setConfigId(messageHookDTO.getBizCode());
				byteTalkConfig.setSign(configMap.getStr("sign"));
				byteTalkConfig.setTokenId(configMap.getStr("tokenId"));
				OaFactory.createAndRegisterOaSender(byteTalkConfig);
			}
			if (StrUtil.equalsAnyIgnoreCase(OaType.WE_TALK.toString(), supplier)) {
				WeTalkConfig weTalkConfig = new WeTalkConfig();
				weTalkConfig.setConfigId(messageHookDTO.getBizCode());
				weTalkConfig.setSign(configMap.getStr("sign"));
				weTalkConfig.setTokenId(configMap.getStr("tokenId"));
				OaFactory.createAndRegisterOaSender(weTalkConfig);
			}
			alarm = OaFactory.getSmsOaBlend(messageHookDTO.getBizCode());
		}

		Request request = new Request();
		request.setContent(messageHookDTO.getMessageContent());
		request.setTitle(messageHookDTO.getMessageTitle());
		request.setMessageUrl(messageHookDTO.getMessageUrl());
		request.setPicUrl(messageHookDTO.getPicUrl());
		request.setIsNoticeAll(messageHookDTO.isNoticeAll());
		request.setPhoneList(messageHookDTO.getPhoneList());

		SysLogDTO sysLog = SysLogUtils.getSysLog();
		Long start = System.currentTimeMillis();
		alarm.senderAsync(request,
				MessageType.valueOf(String.format("%s_%s", supplier, messageHookDTO.getMessageType()).toUpperCase()),
				response -> {
					log.info("发送Hook结果:{}", response);
					sysLog.setTitle(String.format("发送Hook %s", messageHookDTO.getBizCode()));
					sysLog.setLogType(LogTypeEnum.NORMAL.getType());
					sysLog.setServiceId(SystemConfigTypeEnum.WEBHOOK.getValue());
					sysLog.setParams(JacksonSensitiveFieldUtil.writeValueAsString(messageHookDTO));
					sysLog.setTime(System.currentTimeMillis() - start);
					sysLog.setException(JacksonSensitiveFieldUtil.writeValueAsString(response));
					remoteLogService.saveLog(sysLog);
				});
		return R.ok();
	}
	@Service
	@RequiredArgsConstructor
	public static class RemoteLogService {
		private final NamedParameterJdbcTemplate jdbc;
		public void saveLog(SysLogDTO l) { jdbc.update("INSERT INTO sys_log (title, log_type, service_id, params, exception, time_ms) "
				+ "VALUES (:title, :logType, :serviceId, :params, :exception, :time)", new BeanPropertySqlParameterSource(l)); }
	}
}
