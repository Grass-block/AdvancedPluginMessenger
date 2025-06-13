package me.gb2022.apm.remote.protocol;

import io.netty.buffer.ByteBuf;
import me.gb2022.simpnet.packet.DeserializedConstructor;
import me.gb2022.simpnet.packet.Packet;
import me.gb2022.simpnet.util.BufferUtil;

public final class P_Logout implements Packet {
    private final String identifier;

    public P_Logout(String identifier) {
        this.identifier = identifier;
    }

    @DeserializedConstructor
    public P_Logout(ByteBuf data) {
        this.identifier = BufferUtil.readString(data);
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void write(ByteBuf data) {
        BufferUtil.writeString(data, this.identifier);
    }
}
