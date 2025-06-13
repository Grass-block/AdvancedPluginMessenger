package me.gb2022.apm.remote.connector;

import io.netty.buffer.ByteBuf;

public interface ConnectorListener {

    default void messageReceived(RemoteConnector connector, String pid, String channel, String sender, ByteBuf message) {

    }

    default void serverJoined(RemoteConnector connector, String server) {

    }

    default void serverLeft(RemoteConnector connector, String server) {

    }

    default void onMessagePassed(RemoteConnector connector, String pid, String channel, String sender, String receiver, ByteBuf message) {

    }

    default void connectorReady(RemoteConnector connector) {

    }

    default void connectorStopped(RemoteConnector connector) {

    }





    default void endpointLoginResult(RemoteConnector connector, boolean success, String message, String[] servers) {

    }
}
