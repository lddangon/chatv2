package com.chatv2.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global exception handler for the server.
 */
public class ExceptionHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(ExceptionHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            ctx.fireChannelRead(msg);
        } catch (Exception e) {
            log.error("Error in exception handler", e);
            ctx.writeAndFlush("ERROR:Internal server error");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Uncaught exception in channel", cause);
        ctx.writeAndFlush("ERROR:" + cause.getMessage());
    }
}
