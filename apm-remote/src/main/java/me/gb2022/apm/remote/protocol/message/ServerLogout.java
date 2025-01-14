package me.gb2022.apm.remote.protocol.message;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.util.BufferUtil;

public class ServerLogout extends Message {
    private final String id;

    public ServerLogout(String id) {
        this.id = id;
    }

    public ServerLogout(ByteBuf raw) {
        this.id = BufferUtil.readString(raw);
    }

    public String getId() {
        return id;
    }

    @Override
    public void write(ByteBuf data) {
        BufferUtil.writeString(data, id);
    }

    @Override
    public MessageType getType() {
        return MessageType.LOGOUT;
    }
}
