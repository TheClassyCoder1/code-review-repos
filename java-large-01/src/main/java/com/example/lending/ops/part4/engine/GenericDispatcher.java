package com.example.lending.ops.part4.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Synchronous service dispatcher. A service may have a precondition; it runs first and a non-null result from it
 * is returned to the caller without invoking the implementation.
 */
public class GenericDispatcher implements LocalDispatcher {

    public interface ServiceImpl {
        Map<String, Object> invoke(DispatchContext dctx, Map<String, ? extends Object> context) throws GenericServiceException;
    }

    private final Map<String, ServiceImpl[]> services = new ConcurrentHashMap<>();
    private final Delegator delegator;

    public GenericDispatcher(Delegator delegator) { this.delegator = delegator; }

    public void register(String serviceName, ServiceImpl impl) { register(serviceName, impl, null); }

    public void register(String serviceName, ServiceImpl impl, ServiceImpl precondition) {
        services.put(serviceName, new ServiceImpl[] {impl, precondition});
    }

    @Override
    public Map<String, Object> runSync(String serviceName, Map<String, ? extends Object> context) throws GenericServiceException {
        ServiceImpl[] model = services.get(serviceName);
        if (model == null) {
            return ServiceUtil.returnError("Service " + serviceName + " is not defined");
        }
        DispatchContext dctx = new DispatchContext(delegator, this);
        Map<String, Object> callContext = new HashMap<>(context);
        try {
            Map<String, Object> rejection = model[1] == null ? null : model[1].invoke(dctx, callContext);
            return rejection != null ? rejection : model[0].invoke(dctx, callContext);
        } catch (RuntimeException e) {
            throw new GenericServiceException("Service " + serviceName + " failed", e);
        }
    }
}
