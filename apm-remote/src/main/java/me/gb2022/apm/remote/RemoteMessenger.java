package me.gb2022.apm.remote;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import me.gb2022.apm.remote.event.channel.MessageChannel;
import me.gb2022.apm.remote.codec.ObjectCodec;
import me.gb2022.apm.remote.connector.EndpointConnector;
import me.gb2022.apm.remote.connector.ExchangeConnector;
import me.gb2022.apm.remote.connector.RemoteConnector;
import me.gb2022.apm.remote.event.RemoteEventListener;
import me.gb2022.apm.remote.event.connector.ConnectorStartEvent;
import me.gb2022.apm.remote.event.MessengerEventChannel;
import me.gb2022.apm.remote.protocol.packet.D_Raw;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@SuppressWarnings("UnusedReturnValue")
public final class RemoteMessenger {
    private final MessengerEventChannel eventChannel = new MessengerEventChannel(this);
    private final Map<String, MessageChannel> channels = new HashMap<>();
    private final RemoteQuery.Holder queryHolder = new RemoteQuery.Holder();

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
            this.connector = new ExchangeConnector(this.identifier, this.address, this.key);
        } else {
            this.connector = new EndpointConnector(this.identifier, this.address, this.key);
        }
        this.connector.getEventChannel().addListener(this.eventChannel);
        this.eventChannel.addListener(this.queryHolder);
        var evt = new ConnectorStartEvent(this.connector, this.address, this.identifier, this.key, this.proxy);
        this.eventChannel.callEvent(evt, (l, e) -> l.endpointLogin(this, e));
        this.daemonThread = new DaemonThread(this.connector, 5);


        new Thread(this.daemonThread, "APMConnectorDaemon").start();
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


    //handler
    public void registerEventHandler(Object handler) {
        this.eventChannel.addListener(RemoteEventListener.APAdapter.getInstance(handler));
    }

    public void registerEventHandler(Class<?> handler) {
        this.eventChannel.addListener(RemoteEventListener.APAdapter.getInstance(handler));
    }

    public void removeMessageHandler(Object handler) {
        this.eventChannel.removeListener(RemoteEventListener.APAdapter.getInstance(handler));
        RemoteEventListener.APAdapter.clearInstance(handler);
    }

    public void removeMessageHandler(Class<?> handler) {
        this.eventChannel.removeListener(RemoteEventListener.APAdapter.getInstance(handler));
        RemoteEventListener.APAdapter.clearInstance(handler);
    }


    public RemoteConnector connector() {
        return connector;
    }

    public RemoteQuery.Holder queryHolder() {
        return queryHolder;
    }

    public String getIdentifier() {
        return identifier;
    }

    public MessengerEventChannel eventChannel() {
        return eventChannel;
    }

    public MessageChannel messageChannel(String channel) {
        return this.channels.computeIfAbsent(channel, (k) -> {
            var ch = new MessageChannel(this, channel);
            this.eventChannel.addListener(ch);
            return ch;
        });
    }


    //byteBuf
    public String message(String uuid, String target, String channel, ByteBuf msg) {
        return this.connector.sendPacket(new D_Raw(channel, target, msg), uuid);
    }

    public String message(String target, String channel, ByteBuf msg) {
        return message(UUID.randomUUID().toString(), target, channel, msg);
    }

    public String broadcast(String channel, ByteBuf msg) {
        return message(RemoteConnector.BROADCAST_ID, channel, msg);
    }

    public RemoteQuery<ByteBuf> query(String target, String channel, ByteBuf msg) {
        return RemoteQuery.of(this, ByteBuf.class, (uuid) -> message(uuid, target, channel, msg));
    }


    //writer
    public String message(String uuid, String target, String channel, Consumer<ByteBuf> writer) {
        var message = ByteBufAllocator.DEFAULT.buffer();
        writer.accept(message);
        return message(uuid, target, channel, message);
    }

    public String message(String target, String channel, Consumer<ByteBuf> writer) {
        return message(UUID.randomUUID().toString(), target, channel, writer);
    }

    public String broadcast(String channel, Consumer<ByteBuf> writer) {
        return message(RemoteConnector.BROADCAST_ID, channel, writer);
    }

    public RemoteQuery<ByteBuf> query(String target, String channel, Consumer<ByteBuf> writer) {
        return RemoteQuery.of(this, ByteBuf.class, (uuid) -> message(uuid, target, channel, writer));
    }


    //template object
    public <I> String message(String uuid, String target, String channel, I object) {
        return message(uuid, target, channel, (b) -> ObjectCodec.encode(b, object));
    }

    public <I> String message(String target, String channel, I object) {
        return message(UUID.randomUUID().toString(), target, channel, object);
    }

    public <I> String broadcast(String channel, I object) {
        return message(RemoteConnector.BROADCAST_ID, channel, (b) -> ObjectCodec.encode(b, object));
    }

    public <I> RemoteQuery<I> query(String target, String channel, I msg) {
        @SuppressWarnings("unchecked") var a = (RemoteQuery<I>) RemoteQuery.of(
                this,
                msg.getClass(),
                (uuid) -> message(uuid, target, channel, msg)
        );
        return a;
    }


    public static class DaemonThread implements Runnable {
        public static final Logger LOGGER = LogManager.getLogger("APM-MessengerDaemon");
        private final RemoteConnector connector;
        private final int restartInterval;

        private boolean running = true;

        public DaemonThread(RemoteConnector connector, int restartInterval) {
            this.connector = connector;
            this.restartInterval = restartInterval;
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
                        LOGGER.info(
                                "[{}] cannot reach remote connector {}, wait 30s before reconnect.",
                                this.connector.getIdentifier(),
                                this.connector.getBinding()
                        );

                        try {
                            Thread.sleep(30000L);
                        } catch (InterruptedException e2) {
                            throw new RuntimeException(e2);
                        }
                    } else {
                        LOGGER.error("[{}] FOUND ERROR: {}", this.connector.getIdentifier(), e.getMessage());
                        LOGGER.catching(e);
                    }
                }

                if (!this.running) {
                    return;
                }
                LOGGER.warn("[{}] Connector stopped, restart in {} sec.", this.connector.getIdentifier(), this.restartInterval);
                try {
                    Thread.sleep(this.restartInterval * 1000L);
                } catch (InterruptedException e2) {
                    throw new RuntimeException(e2);
                }
                LOGGER.info("[{}] restarting connector thread.", this.connector.getIdentifier());
            }
        }
    }
}
