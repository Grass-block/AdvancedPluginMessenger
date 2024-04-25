package me.gb2022.pluginsmessenger;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.tbstcraft.quark.Quark;
import org.tbstcraft.quark.proxy.protocol.NettyChannelInitializer;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Logger;

public interface _ProxyConnector {
    String PROXY_SERVER_ID = "__proxy";

    int QUERY_TYPE = 0x00;
    int MESSAGE_TYPE = 0x01;

    String sendMessage(String target, String channel, ByteBuf message);

    void connect(InetSocketAddress address);

    void disconnect();

    default void sendQuery(String target, String channel, ByteBuf message, Consumer<ByteBuf> resultHandler) {
        String id = sendMessage(target, channel, message);
        try {
            resultHandler.accept(getMessage(id).get());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    default String sendProxyMessage(String channel, ByteBuf message) {
        return sendMessage(PROXY_SERVER_ID, channel, message);
    }

    default void sendProxyQuery(String channel, ByteBuf message, Consumer<ByteBuf> resultHandler) {
        sendQuery(PROXY_SERVER_ID, channel, message, resultHandler);
    }

    Future<ByteBuf> getMessage(String messageId);

    class ProxyHandlerImpl implements _ProxyConnector {
        private final EventLoopGroup group = new NioEventLoopGroup();
        public Map<String, String> receivedQueries;
        Logger LOGGER = Quark.LOGGER;
        private SocketChannel channel;


        @Override
        public void connect(InetSocketAddress address) {
            new Thread(() -> {
                try {
                    Bootstrap bootstrap = new Bootstrap();
                    bootstrap.group(this.group)
                            .channel(NioSocketChannel.class)
                            .handler(this.initializer());
                    ChannelFuture future = bootstrap.connect(address).sync();
                    LOGGER.info("connected to remote host %s".formatted(address));
                    future.channel().closeFuture().sync();
                } catch (InterruptedException e) {
                    LOGGER.severe(e.getMessage());
                } finally {
                    LOGGER.info("remote bridge %s closed.".formatted(address));
                    this.group.shutdownGracefully();
                }
            }).start();
        }

        private void _sendMessage(int mode);

        public void writeHeaders(ByteBuf buffer) {
            buffer.writeInt(QUERY_TYPE);
        }

        public Future<ByteBuf> query(String channel, String groupTarget, ByteBuf message) {
            String uuid = UUID.randomUUID().toString();
            ByteBuf buffer = ByteBufAllocator.DEFAULT.ioBuffer();


            _ProxyConnector.writeArray(buffer, channel.getBytes(StandardCharsets.UTF_8));

        }

        @Override
        public void disconnect() {
            this.group.shutdownGracefully();
        }


        @Override
        public String sendMessage(String target, String channel, ByteBuf message) {


            buffer.writeBytes()


            return uuid;
        }

        @Override
        public Future<ByteBuf> getMessage(String messageId) {
            return null;
        }

        private ChannelInitializer<SocketChannel> initializer() {
            return new NettyChannelInitializer(new ClientHandler());
        }
    }

    final class ProxyFuture implements Future<ByteBuf> {
        private final ProxyHandlerImpl parent;
        private final String uuid;

        public ProxyFuture(ProxyHandlerImpl parent, String uuid) {
            this.parent = parent;
            this.uuid = uuid;
        }

        @Override
        public ByteBuf get() throws InterruptedException, ExecutionException {
            while (!this.parent.receivedQueries.containsKey(this.uuid)) {
                Thread.yield();
            }
            return this.parent.receivedQueries.get(this.uuid);
        }
    }

    final class ClientHandler extends ChannelInboundHandlerAdapter {

    }


    private static void writeHeaders(ByteBuf buffer, int type, String target) {
        buffer.writeByte(QUERY_TYPE);
        _ProxyConnector.writeArray(buffer, target.getBytes(StandardCharsets.UTF_8));
    }
}





