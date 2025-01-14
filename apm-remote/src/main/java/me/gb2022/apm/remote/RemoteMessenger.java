package me.gb2022.apm.remote;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import me.gb2022.apm.remote.connector.EndpointConnector;
import me.gb2022.apm.remote.connector.ExchangeConnector;
import me.gb2022.apm.remote.connector.RemoteConnector;
import me.gb2022.apm.remote.event.RemoteEventHandler;
import me.gb2022.apm.remote.event.RemoteMessageEventBus;
import me.gb2022.apm.remote.event.ServerListener;
import me.gb2022.apm.remote.event.local.ConnectorReadyEvent;
import me.gb2022.apm.remote.event.remote.*;
import me.gb2022.apm.remote.listen.ConnectorListener;
import me.gb2022.apm.remote.listen.EventChannel;
import me.gb2022.apm.remote.listen.MessageChannel;
import me.gb2022.apm.remote.protocol.packet.data.RawPacket;
import me.gb2022.apm.remote.util.BufferUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class RemoteMessenger implements ServerListener {
    public static final Logger LOGGER = LogManager.getLogger("RemoteMessenger");

    private final Map<String, MessageChannel> channels = new HashMap<>();
    private final RemoteMessageEventBus eventBus = new RemoteMessageEventBus();
    private boolean proxy;
    private String identifier;
    private InetSocketAddress address;
    private byte[] key;
    private RemoteConnector connector;
    private DaemonThread daemonThread;


    public RemoteMessenger(boolean proxy, String identifier, InetSocketAddress address, byte[] key) {
        this.configure(proxy, identifier, address, key);
    }

    public void configure(boolean proxy, String identifier, InetSocketAddress address, byte[] key) {
        this.proxy = proxy;
        this.identifier = identifier;
        this.address = address;
        this.key = key;
        this.restart();
    }

    public void start() {
        if (this.proxy) {
            this.connector = new ExchangeConnector(identifier, address, key, this);
        } else {
            this.connector = new EndpointConnector(identifier, address, key, this);
        }

        this.eventChannel().addListener(new EventAdapter());

        for (var channel : this.channels.values()) {
            channel._setContext(this.connector);
            this.connector.getEventChannel().addListener(channel);
        }

        this.daemonThread = new DaemonThread(this.connector, 5);
        new Thread(this.daemonThread, "APMConnectorDaemon").start();

        //this.connector.waitForReady();
    }

    public void stop() {
        if (this.daemonThread != null) {
            this.daemonThread.stop();
        }
        this.daemonThread = null;
    }

    public void restart() {
        this.stop();
        this.start();
    }


    public Set<String> getServerInGroup() {
        return this.connector.getServerInGroup();
    }


    //handler
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
            LOGGER.catching(e);
        }
    }

    public RemoteConnector getConnector() {
        return connector;
    }

    private void callEvent(Object event, String channel) {
        try {
            this.eventBus.callEvent(event, channel);
        } catch (Exception e) {
            LOGGER.catching(e);
        }
    }


    //message
    public String sendMessage(String target, String channel, ByteBuf msg) {
        var uuid = this.connector.sendPacket((s) -> new RawPacket(channel, target, (b) -> b.writeBytes(msg)));
        msg.release();
        return uuid;
    }

    public String sendMessage(String target, String channel, String msg) {
        return sendMessage(target, channel, (b) -> BufferUtil.writeString(b, msg));
    }

    public String sendMessage(String target, String channel, Consumer<ByteBuf> writer) {
        return this.connector.sendPacket((s) -> new RawPacket(channel, target, writer));
    }

    public void sendBroadcast(String channel, ByteBuf msg) {
        this.sendMessage(RemoteConnector.BROADCAST_ID, channel, msg);
    }

    public void sendBroadcast(String channel, String msg) {
        this.sendMessage(RemoteConnector.BROADCAST_ID, channel, msg);
    }

    public void sendBroadcast(String channel, Consumer<ByteBuf> writer) {
        sendMessage(RemoteConnector.BROADCAST_ID, channel, writer);
    }


    public RemoteConnector.ServerQuery sendQuery(String target, String channel, ByteBuf msg) {
        String uuid = sendMessage(target, channel, msg);
        return new RemoteConnector.ServerQuery(this.connector, uuid);
    }

    public RemoteConnector.ServerQuery sendQuery(String target, String channel, Consumer<ByteBuf> writer) {
        ByteBuf message = ByteBufAllocator.DEFAULT.ioBuffer();
        writer.accept(message);

        return this.sendQuery(target, channel, message);
    }

    public RemoteConnector.ServerQuery sendQuery(String target, String channel, String msg) {
        return sendQuery(target, channel, (b) -> BufferUtil.writeString(b, msg));
    }


    public EventChannel eventChannel() {
        return this.connector.getEventChannel();
    }

    public MessageChannel messageChannel(String s) {
        if (this.channels.containsKey(s)) {
            return this.channels.get(s);
        }

        var channel = new MessageChannel(this.connector, s);
        this.eventChannel().addListener(channel);
        this.channels.put(s, channel);

        return channel;
    }


    public static class DaemonThread implements Runnable {
        private final RemoteConnector connector;
        private final int restartInterval;
        private final Logger logger;

        private boolean running = true;

        public DaemonThread(RemoteConnector connector, int restartInterval) {
            this.connector = connector;
            this.restartInterval = restartInterval;
            this.logger = LogManager.getLogger("APMConnectorDaemon[%s]".formatted(connector.getIdentifier()));
        }

        public RemoteConnector getConnector() {
            return connector;
        }

        public void stop() {
            this.running = false;
            this.getConnector().close();
        }

        @Override
        @SuppressWarnings({"BusyWait"})
        public void run() {
            while (this.running) {
                try {
                    this.getConnector().open();
                } catch (Exception e) {
                    if (e.getMessage().startsWith("Connection refused")) {
                        this.logger.info("cannot reach remote connector {}, wait 30s before reconnect.", this.connector.getBinding());
                        try {
                            Thread.sleep(30000L);
                        } catch (InterruptedException e2) {
                            throw new RuntimeException(e2);
                        }
                    } else {
                        this.logger.catching(e);
                    }
                }

                if (!this.running) {
                    return;
                }
                this.logger.warn("Connector stopped, restart in {} sec.", this.restartInterval);
                try {
                    Thread.sleep(this.restartInterval * 1000L);
                } catch (InterruptedException e2) {
                    throw new RuntimeException(e2);
                }
                this.logger.info("restarting connector thread.");
            }
        }
    }

    private final class EventAdapter implements ConnectorListener {
        @Override
        public void serverJoined(RemoteConnector connector, String server) {
            callEvent(new ServerJoinEvent(connector, server));
        }

        @Override
        public void serverLeft(RemoteConnector connector, String server) {
            callEvent(new ServerQuitEvent(connector, server));
        }

        @Override
        public void connectorReady(RemoteConnector connector) {
            callEvent(new ConnectorReadyEvent(connector));
        }

        @Override
        public void messageReceived(RemoteConnector connector, String pid, String channel, String sender, ByteBuf message) {
            callEvent(new RemoteMessageEvent(connector, sender, message), channel);

            var result = ByteBufAllocator.DEFAULT.ioBuffer();

            callEvent(new RemoteQueryEvent(connector, sender, message, result), channel);

            if (result.writerIndex() != 0) {
                var back = new RawPacket(channel, sender, result);
                back.fillSenderInformation(connector.getIdentifier(), pid);
                connector.sendPacket(back);
            }
        }

        @Override
        public void onMessagePassed(RemoteConnector connector, String pid, String channel, String sender, String receiver, ByteBuf message) {
            var data = message.copy();

            message.readerIndex(0);
            message.writerIndex(0);

            callEvent(new RemoteMessageExchangeEvent(connector, sender, data, message), channel);

            if (message.writerIndex() == 0) {
                message.readerIndex(data.readerIndex());
                message.writerIndex(data.writerIndex());
            }

            data.release();
        }
    }
}
