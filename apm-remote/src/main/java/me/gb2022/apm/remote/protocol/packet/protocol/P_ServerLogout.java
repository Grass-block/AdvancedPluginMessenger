package me.gb2022.apm.remote.protocol.packet.protocol;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.util.BufferUtil;
import me.gb2022.apm.remote.protocol.packet.DeserializedConstructor;
import me.gb2022.apm.remote.protocol.packet.Packet;

public final class P_ServerLogout implements Packet {
    private final String identifier;

    public P_ServerLogout(String identifier) {
        this.identifier = identifier;
    }

    @DeserializedConstructor
    public P_ServerLogout(ByteBuf data) {
        this.identifier = BufferUtil.readString(data);
    }

    @Override
    public void write(ByteBuf data) {
        BufferUtil.writeString(data, this.identifier);
    }

    public String getIdentifier() {
        return identifier;
    }
}
