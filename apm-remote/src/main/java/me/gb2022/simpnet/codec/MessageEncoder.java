package me.gb2022.simpnet.codec;

import io.netty.buffer.ByteBuf;

public interface MessageEncoder<I> {
    void encode(I object, ByteBuf buffer);
}
