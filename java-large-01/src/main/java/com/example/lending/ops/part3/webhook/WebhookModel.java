package com.example.lending.ops.part3.webhook;

import com.example.lending.ops.part3.support.Gson;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import java.util.*;
import java.util.function.*;

/** Types for the ops chat-robot webhooks (arrears escalations, disbursement failures). */
public final class WebhookModel {
    private WebhookModel() { }
    private static final ObjectMapper MAPPER = new ObjectMapper();
    public interface SysMessageService { R sendHook(MessageHookDTO messageHookDTO); }
    @Data public static class MessageHookDTO { private String bizCode, messageType, messageTitle, messageContent, messageUrl, picUrl; private boolean noticeAll; private List<String> phoneList; }
    @Data public static class SysSystemConfigEntity { private String configType, configKey, configValue, configStatus; }
    @Data public static class OaConfig { private String configId, sign, tokenId; }
    public static class DingTalkConfig extends OaConfig { }
    public static class ByteTalkConfig extends OaConfig { }
    public static class WeTalkConfig extends OaConfig { }
    @Data public static class Request { private String content, title, messageUrl, picUrl; private Boolean isNoticeAll; private List<String> phoneList; }
    @Data public static class SysLogDTO { private String title, logType, serviceId, params, exception; private Long time; }
    public enum OaType { DING_TALK, BYTE_TALK, WE_TALK }
    public enum MessageType { DING_TALK_TEXT, DING_TALK_MARKDOWN, BYTE_TALK_TEXT, WE_TALK_TEXT, WE_TALK_MARKDOWN }
    public enum SystemConfigTypeEnum { WEBHOOK; public String getValue() { return "webhook"; } }
    public enum YesNoEnum { YES, NO; public String getCode() { return this == YES ? "1" : "0"; } }
    public enum LogTypeEnum { NORMAL; public String getType() { return "0"; } }
    public record R(int code, String msg) { public static R ok() { return new R(0, "ok"); } public static R failed(String msg) { return new R(1, msg); } }
    public static final class UpmsErrorCodes { public static final String SYS_MESSAGE_SMS_CHANNEL_MISSING = "sys.message.channel.missing"; }
    public static final class MsgUtils { public static String getMessage(String code) { return "no active webhook channel (" + code + ")"; } }
    public static final class SysLogUtils { public static SysLogDTO getSysLog() { return new SysLogDTO(); } }
    public record JSONObject(Map<?, ?> values) { public String getStr(String key) { return Objects.toString(values.get(key), null); } }
    public static final class JSONUtil { public static JSONObject parseObj(String json) { return new JSONObject(new Gson().fromJson(json, Map.class)); } }
    /** Masks personal and secret fields (phone numbers, tokens) before a payload reaches the operation log. */
    public static final class JacksonSensitiveFieldUtil {
        private static final Set<String> SENSITIVE = Set.of("phoneList", "sign", "tokenId");
        public static String writeValueAsString(Object value) {
            try {
                Object tree = MAPPER.convertValue(value, Object.class);
                if (tree instanceof Map<?, ?> map) map.keySet().removeIf(SENSITIVE::contains);
                return MAPPER.writeValueAsString(tree);
            } catch (Exception e) {
                return "<unserializable>";
            }
        }
    }
    public static final class Wrappers { public static <E> LambdaQuery<E> lambdaQuery() { return new LambdaQuery<>(); } }
    public static final class LambdaQuery<E> implements Predicate<E> {
        private Predicate<E> predicate = e -> true;
        public LambdaQuery<E> eq(Function<E, ?> column, Object value) { predicate = predicate.and(e -> Objects.equals(column.apply(e), value)); return this; }
        public boolean test(E entity) { return predicate.test(entity); }
    }
}
