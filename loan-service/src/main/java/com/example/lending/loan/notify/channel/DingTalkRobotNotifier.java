package com.example.lending.loan.notify.channel;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Objects;

/** Sends portfolio alerts to the collections team's DingTalk robot. */
@Component
public class DingTalkRobotNotifier {

    private static final Logger log = LoggerFactory.getLogger(DingTalkRobotNotifier.class);

    private static final String DING_TALK_WEB_HOOK_URL = "https://oapi.dingtalk.com/robot/send?access_token=";

    public static class MarkdownDTO {
        private String title;
        private String text;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    public static class DingTalkWebHookDto {
        private final String msgtype = "markdown";
        private MarkdownDTO markdown;

        public String getMsgtype() { return msgtype; }
        public MarkdownDTO getMarkdown() { return markdown; }
        public void setMarkdown(MarkdownDTO markdown) { this.markdown = markdown; }
    }

    public static class CommonRobotNotifyResp {
        @JsonProperty("errcode")
        private int errCode;
        @JsonProperty("errmsg")
        private String errMsg;

        public int getErrCode() { return errCode; }
        public String getErrMsg() { return errMsg; }
    }

    private final RestTemplate restTemplate;

    public DingTalkRobotNotifier(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    private RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public void send(final AlertReceiver receiver, final AlertContent alert) {
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

    private static String renderContent(AlertContent alert) {
        return "#### " + alert.title() + "\n"
                + "> severity: " + alert.severity() + "\n\n"
                + alert.content() + "\n\n"
                + "fired at " + alert.firedAt();
    }
}
