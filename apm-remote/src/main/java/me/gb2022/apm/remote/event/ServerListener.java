package me.gb2022.apm.remote.event;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.connector.RemoteConnector;

public interface ServerListener {

    default void onServerJoin(RemoteConnector connector, String server) {
    }

    default void onServerLeave(RemoteConnector connector, String server) {
    }

    default void onRemoteQuery(RemoteConnector connector, String sender, String channel, ByteBuf data, ByteBuf result) {
    }

    default void onConnected(RemoteConnector connector) {
    }

    default void onDisconnected(RemoteConnector connector) {
    }
}
