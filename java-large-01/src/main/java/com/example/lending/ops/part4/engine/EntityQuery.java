package com.example.lending.ops.part4.engine;

/** Fluent primary-key lookup against a {@link Delegator}. */
public record EntityQuery(Delegator delegator, String entityName, Object keyValue) {

    public static EntityQuery use(Delegator delegator) { return new EntityQuery(delegator, null, null); }

    public EntityQuery from(String entity) { return new EntityQuery(delegator, entity, keyValue); }

    public EntityQuery where(String primaryKeyField, Object value) { return new EntityQuery(delegator, entityName, value); }

    public GenericValue queryOne() throws GenericEntityException {
        return delegator.findOne(entityName, keyValue == null ? null : keyValue.toString());
    }
}
