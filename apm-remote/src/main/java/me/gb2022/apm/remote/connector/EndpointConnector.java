package me.gb2022.apm.remote.connector;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import me.gb2022.apm.remote.protocol.packet.*;
import me.gb2022.apm.remote.util.NettyChannelInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.Future;

public final class EndpointConnector extends RemoteConnector {
    private static final Logger LOGGER = LogManager.getLogger("APM-EndpointConnector");

    private final byte[] key;
    private ChannelHandlerContext channel;

    public EndpointConnector(String id, InetSocketAddress binding, byte[] key) {
        super(binding, key, id);
        this.key = key;
    }

    @Override
    public void open() {
        var target = getBinding();
        var identifier = getIdentifier();
        var group = new NioEventLoopGroup();
        var bootstrap = new Bootstrap();

        LOGGER.info("[{}]Target: {}.", identifier, target);

        bootstrap.group(group);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(new NettyChannelInitializer(NetworkController::new));

        LOGGER.info("[{}]Pipeline initialized.", identifier);

        try {
            var future = bootstrap.connect(this.getBinding()).sync();
            LOGGER.info("[{}]Connected to target {}, logging in.", identifier, target);

            future.channel().closeFuture().sync();

            LOGGER.info("[{}]Disconnected from target {}. closing APM channel.", identifier, target);
        } catch (InterruptedException e) {
            LOGGER.error("[{}]Handled exception waiting for connector lifecycle.", identifier);
            LOGGER.catching(e);
        } finally {
            group.shutdownGracefully();
            LOGGER.info("[{}]Workgroup closed. ", identifier);
        }
    }

    @Override
    public void close() {
        if (this.channel == null) {
            return;
        }

        super.close();
        LOGGER.info("[{}]Disconnecting from target {}.", this.getIdentifier(), this.getBinding());
        this.channel.disconnect();
    }

    @Override
    public void handlePacket(Packet packet, ChannelHandlerContext ctx) {
        if (packet instanceof P_LoginResult p) {
            if (!p.success()) {
                ctx.disconnect();
                return;
            }

            LOGGER.info("[{}]Login success.", this.getIdentifier());

            this.groupServers.addAll(Arrays.asList(p.servers()));

            this.ready();
            return;
        }

        if (packet instanceof P_ServerLogin p) {
            this.groupServers.add(p.getIdentifier());
            this.eventChannel.serverJoined(this, p.getIdentifier());
        }

        if (packet instanceof P_ServerLogout p) {
            this.groupServers.remove(p.getIdentifier());
            this.eventChannel.serverLeft(this, p.getIdentifier());
        }

        super.handlePacket(packet, ctx);
    }

    @Override
    public ChannelHandlerContext getPacketDest(String receiver) {
        return this.channel;
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    private class NetworkController extends SimpleChannelInboundHandler<ByteBuf> {
        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            channel = ctx;
            sendPacket(new P_Login(getIdentifier(), key), ctx);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            receivePacket(msg, ctx);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
            if (cause.getMessage().contains("Connection reset")) {
                LOGGER.error("[{}]connection reset,disconnection...", identifier);
                ctx.close();
            }
        }
    }
}
