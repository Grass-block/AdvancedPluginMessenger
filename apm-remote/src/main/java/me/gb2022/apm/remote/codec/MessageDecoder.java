package me.gb2022.apm.remote.codec;

import io.netty.buffer.ByteBuf;

public interface MessageDecoder<I> {
    I decode(ByteBuf buffer);
}
