package me.gb2022.apm.remote.protocol.packet.protocol;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.util.BufferUtil;
import me.gb2022.apm.remote.protocol.packet.DeserializedConstructor;
import me.gb2022.apm.remote.protocol.packet.Packet;
import me.gb2022.commons.math.SHA;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class P_Login implements Packet {
    private final String identifier;
    private final String verification;

    public P_Login(String identifier, byte[] key) {
        this.identifier = identifier;
        this.verification = SHA.getSHA256(new String(key, StandardCharsets.UTF_8), false);
    }

    @DeserializedConstructor
    public P_Login(ByteBuf data) {
        this.identifier = BufferUtil.readString(data);
        this.verification = BufferUtil.readString(data);
    }

    public boolean verify(byte[] key) {
        return Objects.equals(this.verification, SHA.getSHA256(new String(key, StandardCharsets.UTF_8), false));
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void write(ByteBuf data) {
        BufferUtil.writeString(data, this.identifier);
        BufferUtil.writeString(data, this.verification);
    }
}
