package com.example.lending.ops.part4.gateway;

/** Dispatches decoded kiosk packets to the handler for their type. */
public class MessageType implements MessageHandler {

    @Override
    public void pingReq(final ChannelHandlerContext ctx) {
        //// todo polymorphism pingReq
        new PingReq().pingReq(ctx);
    }
}
