package me.gb2022.apm.remote.event.remote;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.connector.RemoteConnector;

public final class RemoteMessageExchangeEvent extends RemoteQueryEvent{
    public RemoteMessageExchangeEvent(RemoteConnector connector, String sender, ByteBuf data, ByteBuf result) {
        super(connector, sender, data, result);
    }
}
