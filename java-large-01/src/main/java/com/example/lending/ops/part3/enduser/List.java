package com.example.lending.ops.part3.enduser;

/** List with the sequenced accessor the self-service panels use. */
public interface List<E> extends java.util.List<E> {
    default E getFirst() { return get(0); }
    static <E> List<E> copyOf(java.util.Collection<E> values) {
        final class Values extends java.util.ArrayList<E> implements List<E> { Values() { super(values); } }
        return new Values();
    }
}
