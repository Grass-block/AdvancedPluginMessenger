package me.gb2022.apm.remote.connector;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import me.gb2022.apm.remote.event.ServerListener;
import me.gb2022.apm.remote.util.NettyChannelInitializer;
import me.gb2022.apm.remote.protocol.packet.Packet;
import me.gb2022.apm.remote.protocol.packet.data.DataPacket;
import me.gb2022.apm.remote.protocol.packet.protocol.P_Login;
import me.gb2022.apm.remote.protocol.packet.protocol.P_LoginResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;

public final class EndpointConnector extends RemoteConnector {
    private static final Logger LOGGER = LogManager.getLogger("APMEndpointConnector");

    private final byte[] key;
    private ChannelHandlerContext channel;

    public EndpointConnector(String id, InetSocketAddress binding, byte[] key, ServerListener listener) {
        super(binding, key, id, listener);
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

            this.listener.onConnected(this);
            future.channel().closeFuture().sync();
            this.listener.onDisconnected(this);

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
        LOGGER.info("[{}]Disconnecting from target {}.", this.getIdentifier(), this.getBinding());
        this.channel.disconnect();
    }


    @Override
    public void handlePacket(Packet packet, ChannelHandlerContext ctx) {
        if (packet instanceof P_LoginResult login) {
            if (!login.success()) {
                ctx.disconnect();
                return;
            }

            this.ready();
            return;
        }

        if (packet instanceof DataPacket dp) {
            this.handleDataPacket(dp);
        }
    }

    @Override
    public ChannelHandlerContext getPacketDest(String receiver) {
        return this.channel;
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
    }
}
