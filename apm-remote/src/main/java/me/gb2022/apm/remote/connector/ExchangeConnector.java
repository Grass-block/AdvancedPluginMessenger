package me.gb2022.apm.remote.connector;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import me.gb2022.apm.remote.protocol.APMProtocol;
import me.gb2022.apm.remote.protocol.packet.DataPacket;
import me.gb2022.apm.remote.protocol.packet.P11_ServerLogout;
import me.gb2022.apm.remote.protocol.packet.P20_RawData;
import me.gb2022.commons.container.MultiMap;
import me.gb2022.simpnet.packet.Packet;
import me.gb2022.simpnet.packet.PacketInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.Set;

public final class ExchangeConnector extends RemoteConnector {
    public static final Logger LOGGER = LogManager.getLogger("APM-ExchangeConnector");

    private final MultiMap<String, Channel> channels = new MultiMap<>();
    private Channel channel;

    public ExchangeConnector(InetSocketAddress binding, String id, byte[] key, ConnectorListener listener) {
        super(binding, id, key, listener);
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }

    public void open() {
        this.ready = false;
        var bossGroup = new NioEventLoopGroup();
        var workerGroup = new NioEventLoopGroup();

        try {
            var b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(APMProtocol.server(this).handler(NetworkController::new));
            var future = b.bind(this.getBinding().getPort()).sync();

            this.channel = future.channel();
            this.listener.connectorReady(this);
            this.ready = true;

            LOGGER.info("[{}] server started on {}", this.identifier, this.getBinding());

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

    private Channel[] getChannels() {
        return this.channels.values().toArray(new Channel[0]);
    }

    @Override
    public Channel getPacketDest(String receiver) {
        return this.channels.get(receiver);
    }


    @Override
    public Set<String> getServerInGroup() {
        return this.channels.keySet();
    }

    public void onServerJoin(String id, Channel c) {
        sendPacket(new P11_ServerLogout(id), this.getChannels());
        this.channels.put(id, c);
        this.listener.serverJoined(this, id);
    }

    public void onServerLeft(String id) {
        this.channels.remove(id);
        this.listener.serverLeft(this, id);
        sendPacket(new P11_ServerLogout(id), this.getChannels());
    }


    private class NetworkController extends PacketInboundHandler {
        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if (!channels.containsValue(ctx.channel())) {
                return;
            }
            onServerLeft(channels.of(ctx.channel()));
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Packet packet) {
            if (!(packet instanceof DataPacket data)) {
                return;
            }

            var receiver = data.getReceiver();
            var uuid = data.getUuid();
            var sender = data.getSender();
            var ch = data.getChannel();

            var broadcast = Objects.equals(receiver, ExchangeConnector.BROADCAST_ID);
            var forwarding = !Objects.equals(receiver, getIdentifier());

            if (!forwarding) {
                handlePacket(packet, ctx.channel());
                return;
            }

            if (data instanceof P20_RawData r) {
                var buffer = ByteBufAllocator.DEFAULT.buffer();
                buffer.writeBytes(r.getMessage());
                listener.onMessagePassed(ExchangeConnector.this, uuid, ch, sender, receiver, buffer);
                buffer.readerIndex(0);
                r.setMessage(buffer);
            }

            if (broadcast) {
                data.setReceiver(RemoteConnector.BROADCAST_ACCEPT);
                for (var target : channels.keySet()) {
                    sendPacket(data, channels.get(target));
                }
                return;
            }

            var targetChannel = getPacketDest(data.getReceiver());

            if (targetChannel == null) {
                LOGGER.error("received packet with non-existing dest: {}", data);
                return;
            }

            targetChannel.writeAndFlush(packet);
        }
    }
}
