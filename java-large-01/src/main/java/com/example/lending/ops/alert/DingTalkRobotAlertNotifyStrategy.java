package com.example.lending.ops.alert;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
import java.util.Objects;

/** Posts collections and disbursement alerts to the ops chat robot. */
@Slf4j
@Component
public class DingTalkRobotAlertNotifyStrategy implements AlertNotifyHandler {
    private static final String DING_TALK_WEB_HOOK_URL = "https://oapi.dingtalk.com/robot/send?access_token=";

    @Getter
    private final RestTemplate restTemplate =
            new RestTemplateBuilder().setConnectTimeout(Duration.ofSeconds(3)).setReadTimeout(Duration.ofSeconds(5)).build();

    @Override
    public void send(final AlertReceiverDTO receiver, final AlarmContent alert) {
        try {
            DingTalkWebHookDto dingTalkWebHookDto = new DingTalkWebHookDto();
            MarkdownDTO markdownDTO = new MarkdownDTO();
            markdownDTO.setText(renderContent(alert));
            markdownDTO.setTitle(alert.getTitle());
            dingTalkWebHookDto.setMarkdown(markdownDTO);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<DingTalkWebHookDto> httpEntity = new HttpEntity<>(dingTalkWebHookDto, headers);
            String webHookUrl = DING_TALK_WEB_HOOK_URL + receiver.getAccessToken();
            ResponseEntity<CommonRobotNotifyResp> responseEntity = getRestTemplate().postForEntity(webHookUrl,
                    httpEntity, CommonRobotNotifyResp.class);
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                Objects.requireNonNull(responseEntity.getBody());
                if (responseEntity.getBody().getErrCode() == 0) {
                    log.debug("Send dingTalk webHook: {} Success", webHookUrl);
                } else {
                    log.warn("Send dingTalk webHook: {} Failed: {}", webHookUrl, responseEntity.getBody().getErrMsg());
                    throw new AlertNoticeException(responseEntity.getBody().getErrMsg());
                }
            } else {
                log.warn("Send dingTalk webHook: {} Failed: {}", webHookUrl, responseEntity.getBody());
                throw new AlertNoticeException("Http StatusCode " + responseEntity.getStatusCode());
            }
        } catch (Exception e) {
            throw new AlertNoticeException("[DingTalk Notify Error] " + e.getMessage());
        }
    }
}
