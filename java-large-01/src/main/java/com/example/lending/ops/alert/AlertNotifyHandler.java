package com.example.lending.ops.alert;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/** Delivers ops alerts (failed disbursements, collection-run errors) to a notification channel. */
public interface AlertNotifyHandler {
    void send(AlertReceiverDTO receiver, AlarmContent alert);

    default String renderContent(AlarmContent alert) {
        return "#### " + alert.getTitle() + "\n\n" + alert.getContent();
    }

    @Data
    class AlertReceiverDTO { private String name, accessToken; }
    @Data
    class AlarmContent { private String title, content; }
    @Data
    class MarkdownDTO { private String title, text; }
    @Data
    class DingTalkWebHookDto {
        private String msgtype = "markdown";
        private MarkdownDTO markdown;
    }

    @Data
    class CommonRobotNotifyResp {
        @JsonProperty("errcode")
        private Integer errCode;
        @JsonProperty("errmsg")
        private String errMsg;
    }

    class AlertNoticeException extends RuntimeException {
        public AlertNoticeException(String message) { super(message); }
    }
}
