package me.gb2022.apm.remote.util;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import java.util.function.Supplier;

public final class NettyChannelInitializer extends ChannelInitializer<SocketChannel> {
    private final Supplier<ChannelHandler>[] handlers;
    int FR_L = Integer.MAX_VALUE;
    int LF_OFF = 0;
    int LF_L = 4;
    int LA = 0;
    int B_T_S = 4;

    @SafeVarargs
    public NettyChannelInitializer(Supplier<ChannelHandler>... handlers) {
        this.handlers = handlers;
    }


    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast(new LengthFieldBasedFrameDecoder(FR_L, LF_OFF, LF_L, LA, B_T_S, true));
        pipeline.addLast(new LengthFieldPrepender(LF_L, LA, false));

        for (Supplier<ChannelHandler> handler : this.handlers) {
            pipeline.addLast(handler.get());
        }
    }
}
