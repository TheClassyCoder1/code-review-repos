package com.example.lending.ops.part4.engine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** In-process entity store for the back-office services; relations map a name to an FK field and target entity. */
public class Delegator {

    private record Relation(String fkField, String targetEntity) { }

    private final Map<String, String> primaryKeys = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Map<String, Object>>> rows = new ConcurrentHashMap<>();
    private final Map<String, Relation> relations = new ConcurrentHashMap<>();

    public void defineEntity(String entityName, String primaryKeyField) {
        primaryKeys.put(entityName, primaryKeyField);
        rows.computeIfAbsent(entityName, k -> new ConcurrentHashMap<>());
    }

    public void defineRelation(String entityName, String relationName, String fkField, String targetEntity) {
        relations.put(entityName + "." + relationName, new Relation(fkField, targetEntity));
    }

    public GenericValue findOne(String entityName, String primaryKey) throws GenericEntityException {
        Map<String, Map<String, Object>> table = rows.get(entityName);
        if (table == null) {
            throw new GenericEntityException("Unknown entity " + entityName);
        }
        Map<String, Object> row = primaryKey == null ? null : table.get(primaryKey);
        return row == null ? null : new GenericValue(this, entityName, row);
    }

    GenericValue findRelatedOne(GenericValue value, String relationName) throws GenericEntityException {
        Relation relation = relations.get(value.getEntityName() + "." + relationName);
        if (relation == null) {
            throw new GenericEntityException("No relation " + relationName + " on " + value.getEntityName());
        }
        return findOne(relation.targetEntity(), value.getString(relation.fkField()));
    }

    String primaryKeyOf(GenericValue value) {
        return value.getString(primaryKeys.get(value.getEntityName()));
    }

    void store(GenericValue value) throws GenericEntityException {
        String pk = primaryKeyOf(value);
        if (pk == null) {
            throw new GenericEntityException("Missing primary key for " + value.getEntityName());
        }
        rows.get(value.getEntityName()).put(pk, Map.copyOf(value.fields));
    }
}
