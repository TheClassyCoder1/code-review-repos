package com.example.lending.loan.servicing.collections.debtors;

import java.util.Objects;

/** Identifier scheme requested by an export: which property identifies each debtor. */
public final class IdScheme {

    public enum IdentifiableProperty { ID, NAME, ACCOUNT_NUMBER, EXTERNAL_REFERENCE }

    public static final IdScheme ID = new IdScheme(IdentifiableProperty.ID);
    public static final IdScheme NAME = new IdScheme(IdentifiableProperty.NAME);
    public static final IdScheme ACCOUNT_NUMBER = new IdScheme(IdentifiableProperty.ACCOUNT_NUMBER);
    public static final IdScheme EXTERNAL_REFERENCE = new IdScheme(IdentifiableProperty.EXTERNAL_REFERENCE);

    private final IdentifiableProperty property;

    private IdScheme(IdentifiableProperty property) {
        this.property = property;
    }

    public static IdScheme from(String value) {
        if (value == null || value.isBlank()) {
            return ID;
        }
        return new IdScheme(IdentifiableProperty.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT)));
    }

    public boolean is(IdentifiableProperty other) {
        return property == other;
    }

    public IdentifiableProperty getProperty() {
        return property;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof IdScheme that && property == that.property;
    }

    @Override
    public int hashCode() {
        return Objects.hash(property);
    }
}
