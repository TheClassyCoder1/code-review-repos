package com.example.lending.ops.part3.sortgroup;

/** Lazily fetched query result. */
public interface Cursor<T> extends Iterable<T>, java.io.Closeable {
    @Override
    void close();
}
