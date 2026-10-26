package com.example.lending.ops.part4.gateway;

/** Answers a kiosk keep-alive with PINGRESP. */
public class PingReq implements MessageHandler {

    @Override
    public void pingReq(final ChannelHandlerContext ctx) {
        ctx.writeAndFlush("PINGRESP");
    }
}
