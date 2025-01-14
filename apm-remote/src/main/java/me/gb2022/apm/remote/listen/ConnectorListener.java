package me.gb2022.apm.remote.listen;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.connector.RemoteConnector;

public interface ConnectorListener {

    default void messageReceived(RemoteConnector connector, String pid, String channel, String sender, ByteBuf message) {

    }

    default void messageReceived(RemoteConnector connector, String pid, String channel, String sender, String message) {

    }

    default void serverJoined(RemoteConnector connector, String server) {

    }

    default void serverLeft(RemoteConnector connector, String server) {

    }

    default void onMessagePassed(RemoteConnector connector, String pid, String channel, String sender, String receiver, ByteBuf message) {

    }

    default void connectorReady(RemoteConnector connector) {

    }
}
