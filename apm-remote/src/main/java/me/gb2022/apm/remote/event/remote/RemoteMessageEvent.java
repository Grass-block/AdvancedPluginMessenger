package me.gb2022.apm.remote.event.remote;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.connector.RemoteConnector;

public final class RemoteMessageEvent extends RemoteEvent {
    private final ByteBuf data;

    public RemoteMessageEvent(RemoteConnector connector, String sender, ByteBuf data) {
        super(connector, sender);
        this.data = data;
    }

    public ByteBuf getData() {
        return data;
    }
}
