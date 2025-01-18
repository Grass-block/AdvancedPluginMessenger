package me.gb2022.apm.remote.event;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.RemoteMessenger;
import me.gb2022.apm.remote.connector.RemoteConnector;
import me.gb2022.apm.remote.event.connector.ConnectorReadyEvent;
import me.gb2022.apm.remote.event.connector.EndpointLoginResultEvent;
import me.gb2022.apm.remote.event.message.RemoteMessageEvent;
import me.gb2022.apm.remote.event.message.RemoteMessageSurpassEvent;
import me.gb2022.apm.remote.event.message.RemoteQueryEvent;
import me.gb2022.apm.remote.connector.ConnectorListener;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

public final class MessengerEventChannel implements ConnectorListener {
    private final Set<RemoteEventListener> listeners = new HashSet<>();
    private final RemoteMessenger handle;

    public MessengerEventChannel(RemoteMessenger handle) {
        this.handle = handle;
    }

    @Override
    public void messageReceived(RemoteConnector connector, String pid, String channel, String sender, ByteBuf message) {
        var pass = new RemoteMessageEvent(this.handle.connector(), sender, pid, channel, message);


        for (var listener : this.listeners) {
            listener.remoteMessage(this.handle, pass);
        }

        if(connector.verifyQueryResult(pid)){
            return;
        }

        var query = new RemoteQueryEvent(this.handle.connector(), sender, pid, channel, message);

        for (var listener : this.listeners) {
            listener.remoteQuery(this.handle, query);
        }

        if (query.hasResult()) {
            query.result((b) -> this.handle.message(pid, sender, channel, b));
        }
    }

    @Override
    public void onMessagePassed(RemoteConnector connector, String pid, String channel, String sender, String receiver, ByteBuf message) {
        var event = new RemoteMessageSurpassEvent(this.handle.connector(), pid, channel, sender, message);

        for (var listener : this.listeners) {
            listener.messageSurpassed(this.handle, event);
        }

        if (event.hasResult()) {
            message.writerIndex(0);
            event.result(message::writeBytes);
        }
    }

    @Override
    public void serverJoined(RemoteConnector connector, String server) {
        for (var listener : this.listeners) {
            listener.endpointJoined(this.handle, new EndpointJoinEvent(this.handle.connector(), server));
        }
    }

    @Override
    public void serverLeft(RemoteConnector connector, String server) {
        for (var listener : this.listeners) {
            listener.endpointLeft(this.handle, new EndpointLeftEvent(this.handle.connector(), server));
        }
    }

    @Override
    public void connectorReady(RemoteConnector connector) {
        for (var listener : this.listeners) {
            listener.connectorReady(this.handle, new ConnectorReadyEvent(connector));
        }
    }

    @Override
    public void endpointLoginResult(RemoteConnector connector, boolean success, String message, String[] servers) {
        for (var listener : this.listeners) {
            listener.endpointLoginResult(this.handle, new EndpointLoginResultEvent(this.handle.connector(), success, message, servers));
        }
    }

    public void addListener(RemoteEventListener listener) {
        this.listeners.add(listener);
    }

    public <E> void callEvent(E event, BiConsumer<RemoteEventListener, E> action) {
        for (var listener : this.listeners) {
            action.accept(listener, event);
        }
    }

    public void removeListener(RemoteEventListener listener) {
        this.listeners.remove(listener);
    }

    public Set<RemoteEventListener> getListeners() {
        return listeners;
    }

    public RemoteMessenger getHandle() {
        return handle;
    }
}
