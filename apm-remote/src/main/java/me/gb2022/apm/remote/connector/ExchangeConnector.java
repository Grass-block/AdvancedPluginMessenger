package me.gb2022.apm.remote.connector;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import me.gb2022.apm.remote.protocol.*;
import me.gb2022.commons.container.MultiMap;
import me.gb2022.simpnet.MessageVerifyFailedException;
import me.gb2022.simpnet.packet.InvalidPacketFormatException;
import me.gb2022.simpnet.packet.Packet;
import me.gb2022.simpnet.packet.PacketInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.Objects;

public final class ExchangeConnector extends RemoteConnector {
    public static final Logger LOGGER = LogManager.getLogger("APM-ExchangeConnector");

    private final MultiMap<String, ChannelHandlerContext> contexts = new MultiMap<>();
    private final byte[] key;
    private Channel channel;

    public ExchangeConnector(String id, InetSocketAddress binding, byte[] key) {
        super(binding, key, id);
        this.key = key;
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    public void open() {
        var bossGroup = new NioEventLoopGroup();
        var workerGroup = new NioEventLoopGroup();

        try {
            var b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(APMProtocol.channelBuilder(this.getVerification()).handler(NetworkController::new));
            var future = b.bind(this.getBinding().getPort()).sync();
            this.ready();
            this.channel = future.channel();

            LOGGER.info("[{}]server started on {}", this.identifier, this.getBinding());

            this.channel.closeFuture().sync();
        } catch (InterruptedException e) {
            LOGGER.catching(e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public void close() {
        super.close();
        this.channel.close();
    }

    private ChannelHandlerContext[] getContexts() {
        return this.contexts.values().toArray(new ChannelHandlerContext[0]);
    }

    @Override
    public void handlePacket(Packet packet, ChannelHandlerContext ctx) {
        if (packet instanceof P_Login login) {
            var id = login.getIdentifier();
            var addr = ctx.channel().remoteAddress();

            LOGGER.info("[{}]Protocol verification passed: {}[{}]", this.identifier, addr, id);

            if (!login.verify(this.key)) {
                LOGGER.info("[{}]KEY verification failed: {}[{}]", this.identifier, addr, id);
                handleSuspectedPacket(login, ctx);
                return;
            }

            LOGGER.info("[{}]KEY verification passed: {}[{}]", this.identifier, addr, id);

            sendPacket(new P_ServerLogin(id), getContexts());
            sendPacket(P_LoginResult.succeed(this.contexts.keySet().toArray(new String[0])), ctx);
            this.contexts.put(id, ctx);

            this.eventChannel.serverJoined(this, id);
        }

        if (packet instanceof P_Logout logout) {
            var id = logout.getIdentifier();
            LOGGER.info("Server logout: {}[{}]", ctx.channel().remoteAddress(), id);

            this.contexts.remove(id);
            sendPacket(new P_ServerLogout(id));

            this.eventChannel.serverLeft(this, id);
        }

        if (packet instanceof D_Raw data) {
            var receiver = data.getReceiver();
            var uuid = data.getUuid();
            var sender = data.getSender();
            var channel = data.getChannel();

            var broadcast = Objects.equals(receiver, BROADCAST_ID);
            var redirect = !Objects.equals(receiver, this.getIdentifier());

            if (broadcast || redirect) {
                var buffer = ByteBufAllocator.DEFAULT.buffer();
                buffer.writeBytes(data.getMessage());
                this.eventChannel.onMessagePassed(this, uuid, channel, sender, receiver, buffer);
                data.setMessage(buffer);
            }

            if (broadcast) {
                data.setReceiver(RemoteConnector.BROADCAST_ACCEPT);
                for (var target : this.contexts.keySet()) {
                    sendPacket(packet, this.contexts.get(target));
                }
                handlePacket(data, ctx);
            } else if (redirect) {
                sendPacket(packet, getPacketDest(receiver));
            }
        }

        super.handlePacket(packet, ctx);
    }

    @Override
    public void handleSuspectedPacket(Packet packet, ChannelHandlerContext ctx) {
        if (packet instanceof P_Login login) {
            LOGGER.info("[{}] Suspected login of server: {}[{}]", this.identifier, ctx.channel().remoteAddress(), login.getIdentifier());
            sendPacket(P_LoginResult.failed("verification failed"), ctx);

            ctx.disconnect();
        }
    }

    @Override
    public ChannelHandlerContext getPacketDest(String receiver) {
        return this.contexts.get(receiver);
    }

    private class NetworkController extends PacketInboundHandler {
        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if (!contexts.containsValue(ctx)) {
                return;
            }
            handlePacket(new P_Logout(contexts.of(ctx)), ctx);
        }

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            contexts.put("__self", ctx);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (cause.getMessage().contains("Connection reset")) {
                LOGGER.error("[{}]connection reset, disconnecting...", identifier);
                ctx.close();
            }
            if (cause instanceof MessageVerifyFailedException e) {
                LOGGER.error("[{}]found invalid datapack (sig={}), disconnecting...", identifier, e.getMessage());
                ctx.disconnect();
            }
            if (cause instanceof InvalidPacketFormatException e) {
                LOGGER.error("[{}]found invalid datapack (sig={}), disconnecting...", identifier, e.getMessage());
                ctx.disconnect();
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Packet packet) {
            handlePacket(packet, ctx);
        }
    }
}
