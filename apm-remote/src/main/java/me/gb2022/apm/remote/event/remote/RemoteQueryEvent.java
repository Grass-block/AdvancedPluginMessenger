package me.gb2022.apm.remote.event.remote;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.Server;
import me.gb2022.apm.remote.connector.RemoteConnector;

public final class RemoteQueryEvent extends RemoteEvent {
    private final ByteBuf result;
    private final ByteBuf data;

    public RemoteQueryEvent(RemoteConnector connector, Server sender, ByteBuf data, ByteBuf result) {
        super(connector, sender);
        this.data = data;
        this.result = result;
    }

    public ByteBuf getData() {
        return data;
    }

    public ByteBuf getResult() {
        return result;
    }
}
