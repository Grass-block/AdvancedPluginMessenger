package me.gb2022.apm.remote.protocol.packet.data;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.util.BufferUtil;
import me.gb2022.apm.remote.protocol.packet.DeserializedConstructor;

public final class StringPacket extends DataPacket {
    private final String message;

    public StringPacket(String channel, String receiver, String message) {
        super(channel, receiver);
        this.message = message;
    }

    @DeserializedConstructor
    public StringPacket(ByteBuf data) {
        super(data);
        this.message = BufferUtil.readString(data);
    }

    @Override
    public void write0(ByteBuf data) {
        BufferUtil.writeString(data, this.message);
    }

    public String getMessage() {
        return message;
    }
}
