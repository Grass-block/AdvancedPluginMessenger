package me.gb2022.apm.remote.listen;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.connector.RemoteConnector;

import java.util.HashSet;
import java.util.Set;

public final class EventChannel implements ConnectorListener {
    private final Set<ConnectorListener> listeners = new HashSet<>();

    @Override
    public void messageReceived(RemoteConnector connector, String pid, String channel, String sender, ByteBuf message) {
        for (var listener : this.listeners) {
            listener.messageReceived(connector, pid, channel, sender, message);
        }
    }

    @Override
    public void serverJoined(RemoteConnector connector, String server) {
        for (var listener : this.listeners) {
            listener.serverJoined(connector, server);
        }
    }

    @Override
    public void messageReceived(RemoteConnector connector, String pid, String channel, String sender, String message) {
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
