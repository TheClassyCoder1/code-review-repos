package com.example.lending.ops.part4.engine;

/** What a service implementation sees of the engine: the entity store and a dispatcher for nested calls. */
public record DispatchContext(Delegator getDelegator, LocalDispatcher getDispatcher) {
}
