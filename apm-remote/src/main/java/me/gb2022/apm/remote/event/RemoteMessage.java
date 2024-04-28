package me.gb2022.apm.remote.event;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.object.Server;

public class RemoteMessage {
    private final Server sender;
    private final ByteBuf data;

    public RemoteMessage(Server sender, ByteBuf data) {
        this.sender = sender;
        this.data = data;
    }

    public Server getSender() {
        return sender;
    }

    public ByteBuf getData() {
        return data;
    }
}
