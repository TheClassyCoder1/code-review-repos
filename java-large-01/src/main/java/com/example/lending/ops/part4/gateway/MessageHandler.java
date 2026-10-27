package com.example.lending.ops.part4.gateway;

/** MQTT control-packet handling for the branch kiosks' telemetry link. */
public interface MessageHandler {

    /** The slice of the channel a handler needs: write a frame back to the kiosk. */
    interface ChannelHandlerContext {
        void writeAndFlush(Object frame);
    }

    void pingReq(ChannelHandlerContext ctx);
}
