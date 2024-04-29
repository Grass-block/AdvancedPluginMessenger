package me.gb2022.apm.remote.connector;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import me.gb2022.apm.remote.Server;
import me.gb2022.apm.remote.event.local.ClientConnectorLoginFailedEvent;
import me.gb2022.apm.remote.event.local.ConnectorDisconnectEvent;
import me.gb2022.apm.remote.protocol.MessageType;
import me.gb2022.apm.remote.protocol.NettyChannelInitializer;
import me.gb2022.apm.remote.protocol.message.*;

import java.net.InetSocketAddress;

public class ServerConnector extends RemoteConnector {
    private final byte[] key;
    private final EventLoopGroup group = new NioEventLoopGroup();
    private Channel channel;

    public ServerConnector(String id, InetSocketAddress binding, byte[] key) {
        super(binding, id);
        this.key = key;
    }

    public void connect() {
        new Thread(() -> {
            try {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(this.group).channel(NioSocketChannel.class).handler(new NettyChannelInitializer(Handler::new));
                ChannelFuture future = bootstrap.connect(this.getBinding()).sync();
                this.logger.info("connected to remote host %s".formatted(this.getBinding()));
                future.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                this.logger.severe(e.getMessage());
            } finally {
                this.logger.info("remote bridge %s closed.".formatted(this.getBinding()));
                this.group.shutdownGracefully();
            }
        }).start();
    }

    public void disconnect() {
        callEvent(new ConnectorDisconnectEvent(this));
        this.group.shutdownGracefully();
    }

    @Override
    public void sendMessage(ServerMessage message) {
        sendMessageInternal(message);
    }

    private void sendMessageInternal(Message msg) {
        ByteBuf buffer = ByteBufAllocator.DEFAULT.ioBuffer();
        msg.writeData(buffer);
        this.channel.writeAndFlush(buffer);
    }

    private class Handler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            channel = ctx.channel();
            sendMessageInternal(new ServerLogin(key, getId()));
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            ByteBuf raw = ((ByteBuf) msg);
            MessageType type = MessageType.of(raw.readByte());
            switch (type) {
                case LOGIN_RESULT -> onLoginResult(new ServerLoginResult(raw));
                case LOGIN -> onServerJoin(new ServerLogin(raw));
                case LOGOUT -> onServerLeft(new ServerLogout(raw));
                case MESSAGE, QUERY, QUERY_RESULT -> onMessage(new ServerMessage(type, raw));
            }
        }

        public void onLoginResult(ServerLoginResult message) {
            if (message.isSuccess()) {
                logger.info("successfully logged in to remote server(%s)".formatted(getBinding()));
                for (String s : message.getObjects()) {
                    Server svr = new Server(s, ServerConnector.this);
                    addServer(s, svr);
                }

                ready();
            } else {
                logger.info("failed logging in to remote server(%s)".formatted(getBinding()));
                callEvent(new ClientConnectorLoginFailedEvent(ServerConnector.this));
                disconnect();
            }
        }

        public void onMessage(ServerMessage sm) {
            handleMessage(sm, (_msg) -> {
            });
        }

        public void onServerJoin(ServerLogin message) {
            Server svr = new Server(message.getId(), ServerConnector.this);
            addServer(message.getId(), svr);
        }

        public void onServerLeft(ServerLogout message) {
            removeServer(message.getId());
        }
    }
}
