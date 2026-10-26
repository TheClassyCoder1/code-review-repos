package com.example.lending.ops.part4.engine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ServiceUtil {

    public static final String ERROR_MESSAGE = "errorMessage";

    private ServiceUtil() { }

    public static Map<String, Object> returnSuccess() {
        return new HashMap<>(Map.of("responseMessage", "success"));
    }

    public static Map<String, Object> returnError(String errorMessage) {
        return returnError(errorMessage, null, null, null);
    }

    public static Map<String, Object> returnError(String errorMessage, List<? extends Object> errorMessageList,
                                                  Map<String, ? extends Object> errorMessageMap,
                                                  Map<String, ? extends Object> nestedResult) {
        Object nested = nestedResult == null ? null : nestedResult.get(ERROR_MESSAGE);
        return new HashMap<>(Map.of("responseMessage", "error", ERROR_MESSAGE, nested == null ? errorMessage : errorMessage + ": " + nested));
    }

    public static boolean isError(Map<String, ? extends Object> result) {
        return result == null || "error".equals(result.get("responseMessage"));
    }
}
