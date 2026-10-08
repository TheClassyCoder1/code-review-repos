package com.example.lending.loan.integration.terminal;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Line protocol spoken by in-branch cash repayment terminals. */
public class RepaymentTerminalProtocol {

    private static final int MAX_FRAME_LENGTH = 1024;

    public record TerminalMessage(String terminalId, String type, String payload) {
    }

    public void initChannel(ChannelPipeline pipeline) {
        addProtocolHandlers(pipeline);
    }

    protected void addProtocolHandlers(ChannelPipeline pipeline) {
        pipeline.addLast(new LineBasedFrameDecoder(MAX_FRAME_LENGTH));
        pipeline.addLast(new StringEncoder(StandardCharsets.US_ASCII));
        pipeline.addLast(new TerminalProtocolEncoder());
        pipeline.addLast(new TerminalProtocolDecoder());
    }

    static final class TerminalProtocolDecoder extends MessageToMessageDecoder<ByteBuf> {
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf frame, List<Object> out) {
            String[] parts = frame.toString(StandardCharsets.US_ASCII).split(",", 3);
            if (parts.length == 3) {
                out.add(new TerminalMessage(parts[0], parts[1], parts[2]));
            }
        }
    }

    static final class TerminalProtocolEncoder extends MessageToMessageEncoder<TerminalMessage> {
        @Override
        protected void encode(ChannelHandlerContext ctx, TerminalMessage message, List<Object> out) {
            out.add(message.terminalId() + "," + message.type() + "," + message.payload() + "\r\n");
        }
    }
}
