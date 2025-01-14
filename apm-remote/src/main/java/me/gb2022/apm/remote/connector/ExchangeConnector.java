package me.gb2022.apm.remote.connector;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import me.gb2022.apm.remote.event.ServerListener;
import me.gb2022.apm.remote.util.NettyChannelInitializer;
import me.gb2022.apm.remote.protocol.packet.Packet;
import me.gb2022.apm.remote.protocol.packet.data.DataPacket;
import me.gb2022.apm.remote.protocol.packet.protocol.*;
import me.gb2022.commons.container.MultiMap;

import java.net.InetSocketAddress;
import java.util.Objects;

public final class ExchangeConnector extends RemoteConnector {
    private final MultiMap<String, ChannelHandlerContext> contexts = new MultiMap<>();
    private final byte[] key;
    private Channel channel;

    public ExchangeConnector(String id, InetSocketAddress binding, byte[] key, ServerListener listener) {
        super(binding, key, id, listener);
        this.key = key;
    }

    public void open() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new NettyChannelInitializer(NetworkController::new));
            ChannelFuture f = b.bind(this.getBinding().getPort()).sync();
            this.ready();
            this.channel = f.channel();
            this.channel.closeFuture().sync();
        } catch (InterruptedException e) {
            this.logger.catching(e);
        } finally {
            this.logger.info("server started on %s".formatted(this.getBinding()));
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public void close() {
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

            this.logger.info("Protocol verification passed: {}[{}]", addr, id);

            if (!login.verify(this.key)) {
                this.logger.info("KEY verification failed: {}[{}]", addr, id);
                handleSuspectedPacket(login, ctx);
                return;
            }

            this.logger.info("KEY verification passed: {}[{}]", addr, id);

            sendPacket(new P_ServerLogin(id), getContexts());

            this.contexts.put(id, ctx);

            sendPacket(P_LoginResult.succeed(), ctx);
        }

        if (packet instanceof P_Logout logout) {
            var id = logout.getIdentifier();
            this.logger.info("Server logout: {}[{}]", ctx.channel().remoteAddress(), id);

            this.contexts.remove(id);
            sendPacket(new P_ServerLogout(id));
        }

        if (packet instanceof DataPacket data) {
            var receiver = data.getReceiver();

            if (Objects.equals(receiver, DataPacket.BROADCAST_RECEIVER)) {
                for (var target : this.contexts.keySet()) {
                    data.setReceiver(target);
                    sendPacket(packet, this.contexts.get(target));
                }

                data.setReceiver(this.getIdentifier());
                handlePacket(data, ctx);
                return;
            }

            if (!Objects.equals(receiver, this.getIdentifier())) {
                sendPacket(packet, getPacketDest(receiver));
                return;
            }

            this.handleDataPacket(data);
        }
    }

    @Override
    public void handleSuspectedPacket(Packet packet, ChannelHandlerContext ctx) {
        if (packet instanceof P_Login login) {
            this.logger.info("Suspected login of server: {}[{}]", ctx.channel().remoteAddress(), login.getIdentifier());
            sendPacket(P_LoginResult.failed("verification failed"), ctx);

            ctx.disconnect();
        }
    }

    @Override
    public ChannelHandlerContext getPacketDest(String receiver) {
        return this.contexts.get(receiver);
    }

    private class NetworkController extends SimpleChannelInboundHandler<ByteBuf> {
        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if (!contexts.containsValue(ctx)) {
                return;
            }
            handlePacket(new P_Logout(contexts.of(ctx)), ctx);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) {
            receivePacket(byteBuf, ctx);
        }
    }
}
