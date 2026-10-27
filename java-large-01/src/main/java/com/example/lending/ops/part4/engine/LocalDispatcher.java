package com.example.lending.ops.part4.engine;

import java.util.Map;

public interface LocalDispatcher {
    Map<String, Object> runSync(String serviceName, Map<String, ? extends Object> context) throws GenericServiceException;
}
