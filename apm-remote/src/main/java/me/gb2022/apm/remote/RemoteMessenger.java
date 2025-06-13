package me.gb2022.apm.remote;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import me.gb2022.apm.remote.connector.EndpointConnector;
import me.gb2022.apm.remote.connector.ExchangeConnector;
import me.gb2022.apm.remote.connector.RemoteConnector;
import me.gb2022.apm.remote.event.MessengerEventChannel;
import me.gb2022.apm.remote.event.RemoteEventListener;
import me.gb2022.apm.remote.event.channel.MessageChannel;
import me.gb2022.apm.remote.event.connector.ConnectorReadyEvent;
import me.gb2022.apm.remote.protocol.packet.P20_RawData;
import me.gb2022.apm.remote.util.BlockableDaemon;
import me.gb2022.apm.remote.util.ConnectorState;
import me.gb2022.simpnet.codec.ObjectCodec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@SuppressWarnings("UnusedReturnValue")
public final class RemoteMessenger {
    public static final Logger LOGGER = LogManager.getLogger("APM/RemoteMessenger");

    private final MessengerEventChannel eventChannel = new MessengerEventChannel(this);
    private final Map<String, MessageChannel> channels = new HashMap<>();
    private final RemoteQuery.Holder queryHolder = new RemoteQuery.Holder();
    private final AtomicReference<RemoteConnector> connector = new AtomicReference<>();
    private final AtomicReference<ConnectorState> state = new AtomicReference<>(ConnectorState.CLOSED);
    private final BlockableDaemon<RemoteConnector> daemon = new BlockableDaemon<>(this.connector, this.state, this::createConnector);
    private boolean proxy;
    private String identifier;
    private InetSocketAddress address;
    private byte[] key;

    public RemoteMessenger(boolean proxy, String identifier, InetSocketAddress address, byte[] key) {
        this.eventChannel.addListener(this.queryHolder);
        this.eventChannel.addListener(new RemoteEventListener() {
            @Override
            public void connectorReady(RemoteMessenger messenger, ConnectorReadyEvent event) {
                state.set(ConnectorState.OPENED);
            }
        });
        this.configure(proxy, identifier, address, key);
        this.start();
    }

    public void configure(boolean proxy, String identifier, InetSocketAddress address, byte[] key) {
        LOGGER.info("reconfigured data. waiting async daemon restart.");

        this.proxy = proxy;
        this.identifier = identifier;
        this.address = address;
        this.key = key;

        if (this.connector.get() != null) {
            this.connector.get().close();
        }
    }

    public void start() {
        this.daemon.initialize();
        new Thread(this.daemon).start();
    }

    public void stop() {
        this.daemon.quit();
    }


    private RemoteConnector createConnector() {
        if (this.proxy) {
            return new ExchangeConnector(this.address, this.identifier, this.key, this.eventChannel);
        }
        return new EndpointConnector(this.address, this.identifier, this.key, this.eventChannel);
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
        return connector.get();
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
        return this.connector().sendPacket(new P20_RawData(channel, target, msg), uuid);
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
        @SuppressWarnings("unchecked") var a = (RemoteQuery<I>) RemoteQuery.of(this,
                                                                               msg.getClass(),
                                                                               (uuid) -> message(uuid, target, channel, msg)
        );
        return a;
    }

    @Override
    public int hashCode() {
        var ipHash = this.address.getAddress() != null ? this.address.getAddress().hashCode() : 0;
        var portHash = Integer.hashCode(address.getPort());

        return Objects.hash(ipHash, portHash, this.identifier);
    }
}
