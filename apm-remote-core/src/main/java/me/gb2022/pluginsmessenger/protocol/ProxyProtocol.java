package me.gb2022.pluginsmessenger.protocol;

import io.netty.buffer.ByteBuf;

public interface ProxyProtocol {
    static void writePacketHeaders(ByteBuf buffer, MessageType type, String sender, String channel) {
        buffer.writeInt(type.getId());
        BufferUtil.writeString(buffer, sender);
        BufferUtil.writeString(buffer, channel);
    }
}
