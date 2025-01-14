package me.gb2022.apm.remote.connector;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import me.gb2022.apm.remote.event.RemoteEventHandler;
import me.gb2022.apm.remote.event.RemoteMessageEventBus;
import me.gb2022.apm.remote.event.ServerListener;
import me.gb2022.apm.remote.listen.EventChannel;
import me.gb2022.apm.remote.protocol.packet.Packet;
import me.gb2022.apm.remote.protocol.packet.data.DataPacket;
import me.gb2022.apm.remote.protocol.packet.data.RawPacket;
import me.gb2022.apm.remote.protocol.packet.data.StringPacket;
import me.gb2022.apm.remote.util.MessageVerification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class RemoteConnector {
    public static final String BROADCAST_ID = "_apm://broadcast";
    protected final Logger logger;
    protected final ServerListener listener;
    final EventChannel eventChannel = new EventChannel();
    private final MessageVerification verification;
    private final RemoteMessageEventBus eventBus = new RemoteMessageEventBus();
    private final InetSocketAddress binding;
    private final String identifier;
    private final Map<String, ByteBuf> cache = new HashMap<>();
    private final Set<String> groupServers = new HashSet<>();
    boolean debug;
    private boolean ready = false;

    protected RemoteConnector(InetSocketAddress binding, byte[] key, String identifier, ServerListener listener) {
        this.verification = new MessageVerification(MessageVerification.Mode.AES_ECB, key);
        this.binding = binding;
        this.identifier = identifier;
        this.logger = LogManager.getLogger(toString());
        this.listener = listener;
    }

    public MessageVerification getVerification() {
        return verification;
    }

    public ServerListener getListener() {
        return listener;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public InetSocketAddress getBinding() {
        return binding;
    }


    public void waitForReady() {
        while (!this.ready) {
            Thread.yield();
        }
    }

    protected void ready() {
        this.ready = true;

        this.eventChannel.connectorReady(this);
    }

    public final void debug(boolean debug) {
        this.debug = debug;
    }


    public abstract void open();

    public abstract void close();


    //servers
    public Set<String> getServerInGroup() {
        Set<String> set = new HashSet<>(this.groupServers);
        set.add(this.getIdentifier());
        return set;
    }

    public Set<String> getServers() {
        return this.groupServers;
    }

    public boolean existServer(String id) {
        return this.groupServers.contains(id);
    }

    protected void addServer(String id) {
        this.groupServers.add(id);
        this.listener.onServerJoin(this, id);
    }

    protected void removeServer(String id) {
        this.groupServers.remove(id);
        this.listener.onServerLeave(this, id);
    }


    public void callEvent(Object event) {
        try {
            this.eventBus.callEvent(event, RemoteEventHandler.LISTENER_GLOBAL_EVENT_CHANNEL);
        } catch (Exception e) {
            this.logger.catching(e);
        }
    }


    @Override
    public String toString() {
        return "%s(%s)".formatted(this.getClass().getSimpleName(), this.identifier);
    }


    public final void receivePacket(ByteBuf buffer, ChannelHandlerContext ctx) {
        buffer.readerIndex(0);

        var sign = new byte[buffer.readByte()];
        var data = new byte[buffer.readInt()];

        buffer.readBytes(sign);
        buffer.readBytes(data);

        buffer.readerIndex(0);
        buffer.writerIndex(0);

        if (!this.verification.unpack(buffer, sign, data)) {
            return;
        }

        var packet = Packet.Registry.REGISTRY.decode(buffer);

        handlePacket(packet, ctx);
    }

    public final void sendPacket(Packet packet, ChannelHandlerContext... ctx) {
        if (ctx.length == 0 || ctx[0] == null) {
            return;
        }

        var buffer = ctx[0].alloc().buffer();
        Packet.Registry.REGISTRY.encode(packet, buffer);

        this.verification.pack(buffer);

        for (var c : ctx) {
            c.writeAndFlush(buffer.copy());
        }

        buffer.release();
    }

    public final String sendPacket(Function<String, DataPacket> generator) {
        return sendPacket(generator.apply(this.getIdentifier()));
    }

    public final String sendPacket(DataPacket packet) {
        var uuid = UUID.randomUUID().toString();

        packet.fillSenderInformation(uuid, this.getIdentifier());

        this.sendPacket(packet, getPacketDest(packet.getReceiver()));
        return uuid;
    }

    public final String sendMessage(String channel, String receiver, Consumer<ByteBuf> writer) {
        return sendPacket(new RawPacket(channel, receiver, writer));
    }

    public final String sendMessage(String channel, String receiver, ByteBuf message) {
        return sendPacket(new RawPacket(channel, receiver, message));
    }

    public final String sendMessage(String channel, String receiver, String message) {
        return sendPacket(new StringPacket(channel, receiver, message));
    }

    public abstract void handlePacket(Packet packet, ChannelHandlerContext ctx);

    public void handleSuspectedPacket(Packet packet, ChannelHandlerContext ctx) {
    }

    public abstract ChannelHandlerContext getPacketDest(String receiver);

    protected void handleDataPacket(DataPacket packet) {
        if (!Objects.equals(packet.getReceiver(), this.getIdentifier())) {
            return;
        }

        if (packet instanceof RawPacket p) {
            var bytebuf = ByteBufAllocator.DEFAULT.buffer();
            bytebuf.writeBytes(p.getMessage());
            this.eventChannel.messageReceived(this, p.getUuid(), p.getChannel(), p.getSender(), bytebuf);
            bytebuf.release();
            return;
        }

        if (packet instanceof StringPacket p) {
            this.eventChannel.messageReceived(this, p.getUuid(), p.getChannel(), p.getSender(), p.getMessage());
            return;
        }

        this.logger.info("unhandled data packet: {}", packet);
    }


    public EventChannel getEventChannel() {
        return eventChannel;
    }

    public static final class ServerQuery {
        private final RemoteConnector provider;
        private final String uuid;
        private Runnable timeOut;
        private Consumer<ByteBuf> result;
        private long timeOutMillSeconds;

        public ServerQuery(RemoteConnector provider, String uuid) {
            this.provider = provider;
            this.uuid = uuid;
        }

        public ServerQuery timeout(long mills, Runnable command) {
            this.timeOut = command;
            this.timeOutMillSeconds = mills;
            return this;
        }

        public ServerQuery result(Consumer<ByteBuf> result) {
            this.result = result;
            return this;
        }

        public void sync() {
            long start = System.currentTimeMillis();
            while (!this.provider.cache.containsKey(this.uuid)) {
                if (System.currentTimeMillis() - start > this.timeOutMillSeconds) {
                    this.timeOut.run();
                    return;
                }
                Thread.yield();
            }
            this.result.accept(this.provider.cache.get(this.uuid));
            this.provider.cache.remove(this.uuid);
        }
    }
}
