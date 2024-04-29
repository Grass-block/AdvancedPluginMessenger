package me.gb2022.apm.remote.connector;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import me.gb2022.apm.remote.Server;
import me.gb2022.apm.remote.event.local.ConnectorDisconnectEvent;
import me.gb2022.apm.remote.protocol.MessageType;
import me.gb2022.apm.remote.protocol.NettyChannelInitializer;
import me.gb2022.apm.remote.protocol.message.*;
import me.gb2022.commons.container.MultiMap;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Objects;

public class ProxyConnector extends RemoteConnector {
    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    private final MultiMap<String, ChannelHandlerContext> contexts = new MultiMap<>();
    private final byte[] key;

    public ProxyConnector(String id, InetSocketAddress binding, byte[] key) {
        super(binding, id);
        this.key = key;
    }

    public void start() {
        new Thread(() -> {
            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(this.bossGroup, this.workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new NettyChannelInitializer(Handler::new));
                ChannelFuture f = b.bind(this.getBinding().getPort()).sync();
                this.ready();
                f.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                this.logger.severe(e.getMessage());
            } finally {
                this.logger.info("server closed.");
                this.bossGroup.shutdownGracefully();
                this.workerGroup.shutdownGracefully();
            }
        }).start();
    }

    public void stop() {
        for (String s : new HashSet<>(this.contexts.keySet())) {
            this.disconnect(s);
        }
        callEvent(new ConnectorDisconnectEvent(this));

        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
    }


    public void disconnect(String id) {
        this.removeServer(id);
        this.contexts.get(id).disconnect();
    }

    @Override
    public void sendMessage(ServerMessage message) {
        this.sendMessageInternal(message.getReceiver(), message);
    }

    public void sendMessageInternal(String id, Message msg) {
        ByteBuf buffer = ByteBufAllocator.DEFAULT.ioBuffer();
        msg.writeData(buffer);
        this.contexts.get(id).writeAndFlush(buffer);
    }

    private class Handler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            String id = contexts.of(ctx);
            contexts.remove(id);
            removeServer(id);

            for (String s : getServerInGroup()) {
                if (Objects.equals(s, getId())) {
                    continue;
                }

                sendMessageInternal(s, new ServerLogout(id));
            }
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            ByteBuf raw = ((ByteBuf) msg);
            MessageType type = MessageType.of(raw.readByte());
            switch (type) {
                case LOGIN -> onLoginRequest(new ServerLogin(raw), ctx);
                case MESSAGE, QUERY, QUERY_RESULT -> onMessage(new ServerMessage(type, raw));
            }
        }

        public void onLoginRequest(ServerLogin message, ChannelHandlerContext ctx) {
            String clientId = message.getId();

            if (!message.verifyConnection(key)) {
                logger.info("server login failed: %s(%s)".formatted(clientId, ctx.channel().remoteAddress()));
                ByteBuf buffer = ByteBufAllocator.DEFAULT.ioBuffer();
                new ServerLoginResult(false, new String[0]).writeData(buffer);
                ctx.writeAndFlush(buffer);
                ctx.disconnect();
            }
            logger.info("server login success: %s(%s)".formatted(clientId, ctx.channel().remoteAddress()));

            String[] groupServers = getServerInGroup().toArray(new String[0]);

            addServer(clientId, new Server(clientId, ProxyConnector.this));
            contexts.put(clientId, ctx);

            sendMessageInternal(message.getId(), new ServerLoginResult(true, groupServers));

            for (String s : getServerInGroup()) {
                if (Objects.equals(s, getId())) {
                    continue;
                }
                if (Objects.equals(s, clientId)) {
                    continue;
                }

                sendMessageInternal(s, new ServerLogin(new byte[0], clientId));
            }
        }

        public void onMessage(ServerMessage sm) {
            handleMessage(sm, ProxyConnector.this::sendMessage);
        }
    }
}
