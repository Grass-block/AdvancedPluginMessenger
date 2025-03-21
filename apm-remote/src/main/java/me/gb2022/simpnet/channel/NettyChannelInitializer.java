package me.gb2022.simpnet.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import me.gb2022.simpnet.packet.PacketCodec;
import me.gb2022.simpnet.packet.PacketRegistry;
import me.gb2022.simpnet.util.MessageVerification;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class NettyChannelInitializer extends ChannelInitializer<Channel> {
    private final List<Supplier<ChannelHandler>> handlers = new ArrayList<>();

    @SafeVarargs
    public NettyChannelInitializer(Supplier<ChannelHandler>... handlers) {
        this.handlers.addAll(List.of(handlers));
    }

    @Override
    protected void initChannel(Channel channel) {
        for (Supplier<ChannelHandler> handler : this.handlers) {
            channel.pipeline().addLast(handler.get());
        }
    }

    public NettyChannelInitializer config(Consumer<NettyChannelInitializer> handler) {
        handler.accept(this);
        return this;
    }

    public void lengthFrame() {
        var FR_L = Integer.MAX_VALUE;
        var LF_OFF = 0;
        var LF_L = 4;
        var LA = 0;
        var B_T_S = 4;

        this.handlers.add(() -> new LengthFieldBasedFrameDecoder(FR_L, LF_OFF, LF_L, LA, B_T_S, true));
        this.handlers.add(() -> new LengthFieldPrepender(LF_L, LA, false));
    }

    public void packet(PacketRegistry registry) {
        this.handlers.add(() -> new PacketCodec(registry));
    }

    public void encryption(MessageVerification verification) {
        this.handlers.add(() -> new EncryptionCodec(verification));
    }

    public void compression(int threshold, int level) {
        this.handlers.add(() -> new CompressionCodec(threshold, level));
    }

    public NettyChannelInitializer handler(Supplier<ChannelHandler> handler) {
        this.handlers.add(handler);
        return this;
    }
}
