package me.gb2022.apm.remote.protocol.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import me.gb2022.apm.remote.connector.RemoteConnector;
import me.gb2022.simpnet.MessageVerifyFailedException;
import me.gb2022.simpnet.packet.InvalidPacketFormatException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class CommonExceptionHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LogManager.getLogger("APM/ExceptionHandler");
    private final RemoteConnector connector;

    public CommonExceptionHandler(RemoteConnector connector) {
        this.connector = connector;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause.getMessage().contains("Connection reset")) {
            LOGGER.error("[{}]connection reset, disconnecting...", this.connector.getIdentifier());
            LOGGER.catching(cause);
            ctx.disconnect();
        } else if (cause instanceof MessageVerifyFailedException e) {
            LOGGER.error("[{}]failed to verify datapack (sig={}), disconnecting...", this.connector.getIdentifier(), e.getMessage());
            LOGGER.catching(cause);
            ctx.disconnect();
        } else if (cause instanceof InvalidPacketFormatException e) {
            LOGGER.error("[{}]found invalid datapack (sig={}), disconnecting...", this.connector.getIdentifier(), e.getMessage());
            LOGGER.catching(cause);
            ctx.disconnect();
        } else {
            LOGGER.error("[{}]unexpected exception:", this.connector.getIdentifier(), cause);
            LOGGER.catching(cause);
        }
    }
}
