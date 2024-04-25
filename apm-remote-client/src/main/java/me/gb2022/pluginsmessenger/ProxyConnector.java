package me.gb2022.pluginsmessenger;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.tbstcraft.quark.Quark;
import org.tbstcraft.quark.proxy.protocol.Connector;
import org.tbstcraft.quark.proxy.protocol.DispatchedRequestHandler;
import org.tbstcraft.quark.proxy.protocol.NettyChannelInitializer;

import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class ProxyConnector extends Connector {
    public static final Logger LOGGER = Quark.LOGGER;

    private final EventLoopGroup group = new NioEventLoopGroup();
    private final ClientMessageHandler handler = new ClientMessageHandler();

    void connect(InetSocketAddress address) {
        new Thread(() -> {
            try {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(this.group)
                        .channel(NioSocketChannel.class)
                        .handler(new NettyChannelInitializer(new ClientHandler(this)));
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

    void disconnect() {
        this.group.shutdownGracefully();
    }

    public void receive(ByteBuf rawMessage) {
        this.handler.dispatch(rawMessage);
    }


    static final class ClientHandler extends ChannelInboundHandlerAdapter {
        private final ProxyConnector connector;

        ClientHandler(ProxyConnector connector) {
            this.connector = connector;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            this.connector.receive(((ByteBuf) msg));
        }
    }

    static final class ClientMessageHandler implements DispatchedRequestHandler {

        @Override
        public void onQuery(String sender, String channel, ByteBuf request, String uuid) {

        }

        @Override
        public void onMessage(String sender, String channel, ByteBuf message) {

        }
    }
}
