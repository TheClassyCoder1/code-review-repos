package com.example.lending.ops.part5.proto;

/**
 * Base builder. Generated builders override {@link #mergeFrom(Message)} to
 * handle their own type; anything else reaching this method is a type mismatch.
 */
public abstract class AbstractMessageBuilder<B extends AbstractMessageBuilder<B>> implements Message.Builder<B> {

    protected abstract String expectedTypeName();

    @Override
    @SuppressWarnings("unchecked")
    public B mergeFrom(Message other) {
        if (other == null) {
            throw new NullPointerException("other");
        }
        if (!expectedTypeName().equals(other.getTypeName())) {
            throw new IllegalArgumentException("Cannot merge " + other.getTypeName()
                    + " into " + expectedTypeName());
        }
        return (B) this;
    }
}
