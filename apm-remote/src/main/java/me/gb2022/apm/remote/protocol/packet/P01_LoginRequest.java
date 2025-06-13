package me.gb2022.apm.remote.protocol.packet;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.protocol.ChannelState;
import me.gb2022.simpnet.packet.DeserializedConstructor;
import me.gb2022.simpnet.util.BufferUtil;

public final class P01_LoginRequest implements LoginPacket {
    private final String id;

    public P01_LoginRequest(String id) {
        this.id = id;
    }

    @DeserializedConstructor
    public P01_LoginRequest(ByteBuf buffer) {
        this.id = BufferUtil.readString(buffer);
    }

    @Override
    public void write(ByteBuf byteBuf) {
        BufferUtil.writeString(byteBuf, this.id);
    }

    public String getId() {
        return id;
    }


    @Override
    public ChannelState state() {
        return ChannelState.INITIALIZED;
    }
}
