package com.example.lending.loan.integration.collector;

public record ClusterMessage(String identity, Direction direction, String msg) {

    public enum Direction { REQUEST, RESPONSE }

    public String getMsg() { return msg; }
    public Direction getDirection() { return direction; }
    public String getIdentity() { return identity; }
}
