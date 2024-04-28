package me.gb2022.apm.remote.protocol.message;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.protocol.BufferUtil;
import me.gb2022.commons.math.SHA;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ServerLogin extends Message {
    private final String keySHA;
    private final String id;

    public ServerLogin(byte[] key, String id) {
        this.keySHA = SHA.getSHA512(new String(key, StandardCharsets.UTF_8), false);
        this.id = id;
    }

    public ServerLogin(ByteBuf data) {
        this.keySHA = BufferUtil.readString(data);
        this.id = BufferUtil.readString(data);
    }

    public boolean verifyConnection(byte[] key) {
        return Objects.equals(this.keySHA, SHA.getSHA512(new String(key, StandardCharsets.UTF_8), false));
    }

    @Override
    public void write(ByteBuf data) {
        BufferUtil.writeString(data, this.keySHA);
        BufferUtil.writeString(data, this.id);
    }

    @Override
    public EnumMessages getType() {
        return EnumMessages.LOGIN;
    }

    public String getId() {
        return id;
    }
}
