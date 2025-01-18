package me.gb2022.apm.remote.codec;

import io.netty.buffer.ByteBuf;

public interface MessageEncoder<I> {
    void encode(I object, ByteBuf buffer);
}
