package com.example.lending.ops.context;

import com.example.lending.ops.security.LoginUserUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory store for workflow contexts shared between underwriting steps. */
@Service
public class ContextService {

    @Value
    public static class ContextID { String contextId; }

    public enum ContextType { METADATA, DATA, RESOURCE, OBJECT, ENV }

    public record ContextKeyValue(String key, ContextType contextType, String value) { }

    public interface ContextHTTPConstant {
        String CONTEXT_ID_STR = "contextID", CONTEXT_KEY_VALUE_STR = "contextKeyValue";
        String CONTEXT_KEY_TYPE_STR = "contextKeyType", CONTEXT_KEY_PREFIX_STR = "keyPrefix";
    }

    @Getter
    @AllArgsConstructor
    public enum CsServerErrorCodeSummary {
        PARAMS_CANNOT_EMPTY(80015, "{0} cannot be empty");
        private final int errorCode;
        private final String errorDesc;
    }

    public static class CSErrorException extends Exception {
        public CSErrorException(int errCode, String desc) { super(errCode + ": " + desc); }
    }

    @Slf4j
    public static final class ModuleUserUtils {
        /** Resolves the calling user and records the operation in the audit log. */
        public static String getOperationUser(HttpServletRequest req, String msg) {
            String userId = LoginUserUtils.getLoginUser().userId();
            log.info("user {} operation [{}] on {}", userId, msg, req.getRequestURI());
            return userId;
        }
    }

    private final Map<String, Map<String, ContextKeyValue>> values = new ConcurrentHashMap<>();

    public void setValue(ContextID id, ContextKeyValue kv) {
        values.computeIfAbsent(id.getContextId(), k -> new ConcurrentHashMap<>()).put(kv.key(), kv);
    }

    public void removeAllValueByKeyPrefixAndContextType(ContextID id, ContextType type, String keyPrefix) {
        values.getOrDefault(id.getContextId(), new ConcurrentHashMap<>()).values()
                .removeIf(kv -> kv.contextType() == type && kv.key().startsWith(keyPrefix));
    }
}
