package com.example.lending.loan.export;

import java.util.Objects;

public class ReportKey<T> {

    private final T value;

    public ReportKey(T value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReportKey)) {
            return false;
        }
        @SuppressWarnings("rawtypes")
        ReportKey key = (ReportKey) o;
        return Objects.equals(value, key.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
