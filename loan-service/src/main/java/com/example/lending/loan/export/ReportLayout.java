package com.example.lending.loan.export;

import java.util.*;

public class ReportLayout {

    public record Field(String name, String label) {
    }

    public static class FieldList {
        private final List<Field> fields = new ArrayList<>();

        public Field getField(String name) {
            return fields.stream().filter(f -> f.name().equals(name)).findFirst().orElse(null);
        }

        public void add(Field field) { fields.add(field); }
        public void remove(Field field) { fields.remove(field); }
        public List<Field> asList() { return List.copyOf(fields); }
    }

    private final FieldList fieldList = new FieldList();

    public static ReportLayout collections() {
        ReportLayout layout = new ReportLayout();
        for (String column : List.of("loan_id", "tier", "amount", "region", "due_date", "status")) {
            layout.fieldList.add(new Field(column, column.replace('_', ' ')));
        }
        return layout;
    }

    public ReportLayout withOverrides(List<Field> overrides) {
        ReportLayout copy = new ReportLayout();
        fieldList.asList().forEach(copy.fieldList::add);
        if (overrides != null) {
            overrides.forEach(field -> replaceField(copy.fieldList, field));
        }
        return copy;
    }

    protected static void replaceField(FieldList fieldList, Field field) {
        if (field == null) {
            return;
        }
        Field existingField = fieldList.getField(field.name());
        if (existingField != null) {
            fieldList.remove(existingField);
        }
        fieldList.add(field);
    }

    public List<Field> getFields() {
        return fieldList.asList();
    }
}
