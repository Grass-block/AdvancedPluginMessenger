package me.gb2022.apm.remote.connector;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import me.gb2022.apm.remote.APMLoggerManager;
import me.gb2022.apm.remote.Server;
import me.gb2022.apm.remote.event.RemoteEventHandler;
import me.gb2022.apm.remote.event.RemoteMessageEventBus;
import me.gb2022.apm.remote.event.local.ConnectorReadyEvent;
import me.gb2022.apm.remote.event.remote.RemoteMessageEvent;
import me.gb2022.apm.remote.event.remote.RemoteQueryEvent;
import me.gb2022.apm.remote.event.remote.ServerJoinEvent;
import me.gb2022.apm.remote.event.remote.ServerQuitEvent;
import me.gb2022.apm.remote.protocol.MessageType;
import me.gb2022.apm.remote.protocol.message.ServerMessage;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

public abstract class RemoteConnector {
    protected final Logger logger;

    private final RemoteMessageEventBus eventBus = new RemoteMessageEventBus();

    private final HashMap<String, Server> servers = new HashMap<>();
    private final InetSocketAddress binding;
    private final String id;
    private boolean ready = false;

    protected RemoteConnector(InetSocketAddress binding, String id) {
        this.binding = binding;
        this.id = id;
        this.logger = APMLoggerManager.createLogger("APM/" + this.getClass().getSimpleName() + "[%s]".formatted(id));
    }

    public InetSocketAddress getBinding() {
        return binding;
    }

    public Server getServer(String id) {
        return this.servers.get(id);
    }

    public void addServer(String id, Server server) {
        this.servers.put(id, server);
        callEvent(new ServerJoinEvent(this, server));
    }

    public void removeServer(String id) {
        Server svr = getServer(id);
        this.servers.remove(id);
        callEvent(new ServerQuitEvent(this, svr));
    }

    public String getId() {
        return this.id;
    }

    public Map<String, Server> getServers() {
        return this.servers;
    }

    public void waitForReady() {
        while (!this.ready) {
            Thread.yield();
        }
    }

    void ready() {
        this.ready = true;
        this.callEvent(new ConnectorReadyEvent(this));
    }

    public abstract void sendMessage(ServerMessage message);

    public Set<String> getServerInGroup() {
        Set<String> set = new HashSet<>(getServers().keySet());
        set.add(this.getId());
        return set;
    }

    public void handleMessage(ServerMessage message, Consumer<ServerMessage> notMatch) {
        if (!Objects.equals(message.getReceiver(), this.id)) {
            notMatch.accept(message);
            return;
        }

        Server sender = this.getServer(message.getSender());
        ByteBuf data = message.getData();
        String channel = message.getChannel();

        switch (message.getType()) {
            case MESSAGE -> this.callEvent(new RemoteMessageEvent(this, sender, data), channel);
            case QUERY -> {
                ByteBuf result = ByteBufAllocator.DEFAULT.buffer();
                this.eventBus.callEvent(new RemoteQueryEvent(this, sender, data, result), channel);
                if (result.writerIndex() == 0) {
                    return;
                }
                sendMessage(new ServerMessage(MessageType.QUERY_RESULT, this.getId(), message.getSender(), channel, message.getUuid(), result));
            }
            case QUERY_RESULT -> this.getServer(message.getSender()).handleRemoteQueryResult(message.getUuid(), data);
        }
    }

    public void addMessageHandler(Object handler) {
        this.eventBus.registerEventListener(handler);
    }

    public void removeMessageHandler(Object handler) {
        this.eventBus.unregisterEventListener(handler);
    }

    public void callEvent(Object event) {
        try {
            this.eventBus.callEvent(event, RemoteEventHandler.LISTENER_GLOBAL_EVENT_CHANNEL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void callEvent(Object event, String channel) {
        try {
            this.eventBus.callEvent(event, channel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
