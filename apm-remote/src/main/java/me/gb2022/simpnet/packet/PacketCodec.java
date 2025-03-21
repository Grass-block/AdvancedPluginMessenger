package me.gb2022.simpnet.packet;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import java.util.List;

public final class PacketCodec extends ByteToMessageCodec<Packet> {
    private final PacketRegistry registry;

    public PacketCodec(PacketRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet msg, ByteBuf out) {
        this.registry.encode(msg, out);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        out.add(this.registry.decode(in));
    }
}
