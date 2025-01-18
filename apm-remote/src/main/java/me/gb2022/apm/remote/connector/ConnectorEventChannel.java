package me.gb2022.apm.remote.connector;

import io.netty.buffer.ByteBuf;

import java.util.HashSet;
import java.util.Set;

public final class ConnectorEventChannel implements ConnectorListener {
    private final Set<ConnectorListener> listeners = new HashSet<>();

    @Override
    public void messageReceived(RemoteConnector connector, String pid, String channel, String sender, ByteBuf message) {
        for (var listener : this.listeners) {
            listener.messageReceived(connector, pid, channel, sender, message);
        }
    }

    @Override
    public void onMessagePassed(RemoteConnector connector, String pid, String channel, String sender, String receiver, ByteBuf message) {
        for (var listener : this.listeners) {
            listener.onMessagePassed(connector, pid, channel, sender, receiver, message);
        }
    }

    @Override
    public void serverJoined(RemoteConnector connector, String server) {
        for (var listener : this.listeners) {
            listener.serverJoined(connector, server);
        }
    }

    @Override
    public void serverLeft(RemoteConnector connector, String server) {
        for (var listener : this.listeners) {
            listener.serverLeft(connector, server);
        }
    }

    @Override
    public void connectorReady(RemoteConnector connector) {
        for (var listener : this.listeners) {
            listener.connectorReady(connector);
        }
    }

    @Override
    public void endpointLoginResult(RemoteConnector connector, boolean success, String message, String[] servers) {
        for (var listener : this.listeners) {
            listener.endpointLoginResult(connector, success, message, servers);
        }
    }

    public void addListener(ConnectorListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(ConnectorListener listener) {
        this.listeners.remove(listener);

    }

    public Set<ConnectorListener> getListeners() {
        return listeners;
    }
}
