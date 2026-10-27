package com.example.lending.ops.part4.engine;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/** A single row of an entity held by a {@link Delegator}. */
public class GenericValue {

    private final Delegator delegator;
    private final String entityName;
    final Map<String, Object> fields;

    GenericValue(Delegator delegator, String entityName, Map<String, Object> values) {
        this.delegator = delegator;
        this.entityName = entityName;
        this.fields = new HashMap<>(values);
    }

    public String getEntityName() { return entityName; }

    public Object get(String name) { return fields.get(name); }

    public void set(String name, Object value) { fields.put(name, value); }

    public String getString(String name) {
        Object value = fields.get(name);
        return value == null ? null : value.toString();
    }

    public BigDecimal getBigDecimal(String name) {
        Object value = fields.get(name);
        return value == null || value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(value.toString());
    }

    public GenericValue getRelatedOne(String relationName, boolean useCache) throws GenericEntityException {
        return delegator.findRelatedOne(this, relationName);
    }

    public void refresh() throws GenericEntityException {
        GenericValue latest = delegator.findOne(entityName, delegator.primaryKeyOf(this));
        if (latest == null) {
            throw new GenericEntityException(entityName + " row no longer exists");
        }
        fields.clear();
        fields.putAll(latest.fields);
    }

    public void store() throws GenericEntityException { delegator.store(this); }
}
