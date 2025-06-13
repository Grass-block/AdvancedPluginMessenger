package me.gb2022.apm.remote.connector;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import me.gb2022.apm.remote.protocol.APMProtocol;
import me.gb2022.apm.remote.protocol.packet.P10_ServerLogin;
import me.gb2022.apm.remote.protocol.packet.P11_ServerLogout;
import me.gb2022.simpnet.packet.Packet;
import me.gb2022.simpnet.packet.PacketInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

public final class EndpointConnector extends RemoteConnector {
    private static final Logger LOGGER = LogManager.getLogger("APM/EndpointConnector");

    private final Set<String> groupServers = new HashSet<>();
    private Channel channel;

    public EndpointConnector(InetSocketAddress binding, String id, byte[] key, ConnectorListener listener) {
        super(binding, id, key, listener);
    }

    @Override
    public void open() {
        var target = getBinding();
        var identifier = getIdentifier();
        var group = new NioEventLoopGroup();
        var bootstrap = new Bootstrap();

        LOGGER.info("[{}]Connecting to {} with id {}.", identifier, target, identifier);
        bootstrap.group(group);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(APMProtocol.client(this).handler(NetworkController::new));
        LOGGER.info("[{}]Pipeline initialized, connecting...", identifier);

        try {
            bootstrap.connect(this.getBinding()).sync().channel().closeFuture().sync();

            LOGGER.info("[{}]Disconnected from target {}.", identifier, target);
        } catch (InterruptedException e) {
            LOGGER.error("[{}]Handled exception while waiting.", identifier);
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
        LOGGER.info("[{}]Actively disconnecting from target {}.", this.getIdentifier(), this.getBinding());
        this.channel.disconnect();
    }

    @Override
    public void handlePacket(Packet packet, Channel ctx) {
        if (packet instanceof P10_ServerLogin p) {
            this.groupServers.add(p.getIdentifier());
            this.listener.serverJoined(this, p.getIdentifier());
        }

        if (packet instanceof P11_ServerLogout p) {
            this.groupServers.remove(p.getIdentifier());
            this.listener.serverLeft(this, p.getIdentifier());
        }

        super.handlePacket(packet, ctx);
    }

    public void onLogin() {
        this.listener.connectorReady(this);
        this.ready = true;
    }

    public void onLogout() {
        this.listener.connectorStopped(this);
    }


    public void sendPacket(Packet packet, String destination) {
        getPacketDest(destination).writeAndFlush(packet);
    }


    @Override
    public Channel getPacketDest(String receiver) {
        return this.channel;
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    @Override
    public Set<String> getServerInGroup() {
        return this.groupServers;
    }

    private class NetworkController extends PacketInboundHandler {
        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            channel = ctx.channel();
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Packet packet) {
            handlePacket(packet, ctx.channel());
        }
    }
}
