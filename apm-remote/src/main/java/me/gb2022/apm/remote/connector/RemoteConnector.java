package me.gb2022.apm.remote.connector;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import me.gb2022.apm.remote.event.DebugListener;
import me.gb2022.apm.remote.protocol.APMProtocol;
import me.gb2022.apm.remote.protocol.D_DataPacket;
import me.gb2022.apm.remote.protocol.D_Raw;
import me.gb2022.simpnet.packet.Packet;
import me.gb2022.simpnet.packet.PacketRegistry;
import me.gb2022.simpnet.util.MessageVerification;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;

public abstract class RemoteConnector {
    public static final String BROADCAST_ID = "_apm://broadcast";
    public static final String BROADCAST_ACCEPT = "_apm://broadcast/accept";
    protected final ConnectorEventChannel eventChannel = new ConnectorEventChannel();
    protected final Set<String> groupServers = new HashSet<>();
    protected final String identifier;
    private final Set<String> sentMessages = new HashSet<>();
    private final MessageVerification verification;
    private final InetSocketAddress binding;
    boolean debug;
    private boolean ready = false;

    protected RemoteConnector(InetSocketAddress binding, byte[] key, String identifier) {
        this.verification = new MessageVerification(MessageVerification.Mode.AES_ECB, key, APMProtocol.MAGIC_NUMBER);
        this.binding = binding;
        this.identifier = identifier;
    }

    public abstract Logger getLogger();

    public MessageVerification getVerification() {
        return verification;
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

    public void close() {
        this.ready = false;
    }


    //servers
    public Set<String> getServerInGroup() {
        Set<String> set = new HashSet<>(this.groupServers);
        set.add(this.getIdentifier());
        return set;
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

        var packet = PacketRegistry.REGISTRY.decode(buffer);

        handlePacket(packet, ctx);
    }

    public final void sendPacket(Packet packet, ChannelHandlerContext... ctx) {
        for (var c : ctx) {
            if (c == null) {
                continue;
            }
            c.writeAndFlush(packet);
        }
    }

    public final String sendPacket(D_DataPacket packet, String uuid) {
        packet.fillSenderInformation(uuid, this.getIdentifier());

        if (this.debug) {
            getLogger().info("[out] {}", packet);
        }

        if (this instanceof ExchangeConnector && Objects.equals(packet.getReceiver(), BROADCAST_ID)) {
            this.handlePacket(packet, getPacketDest("__self"));
            return uuid;
        }

        this.sentMessages.add(uuid);

        this.sendPacket(packet, getPacketDest(packet.getReceiver()));
        return uuid;
    }

    public ConnectorEventChannel getEventChannel() {
        return eventChannel;
    }


    //implementation
    public void handlePacket(Packet packet, ChannelHandlerContext ctx) {
        if (!(packet instanceof D_DataPacket dp)) {
            return;
        }

        if (this.debug) {
            getLogger().info("[in] {}", packet);
        }

        var receive = Objects.equals(dp.getReceiver(), this.getIdentifier());
        var acceptBroadcast = Objects.equals(dp.getReceiver(), RemoteConnector.BROADCAST_ACCEPT);
        var denySelfSent = Objects.equals(dp.getSender(), this.getIdentifier());

        if (!receive && !acceptBroadcast) {
            return;
        }
        if (denySelfSent) {
            return;
        }

        if (packet instanceof D_Raw p) {
            var buffer = ByteBufAllocator.DEFAULT.buffer();
            buffer.writeBytes(p.getMessage());

            this.eventChannel.messageReceived(this, p.getUuid(), p.getChannel(), p.getSender(), buffer);
            buffer.release();
            return;
        }

        getLogger().warn("[{}] unhandled data packet: {}", this.identifier, packet);
    }

    public abstract ChannelHandlerContext getPacketDest(String receiver);

    public void handleSuspectedPacket(Packet packet, ChannelHandlerContext ctx) {
    }

    public boolean verifyQueryResult(String pid) {
        if (this.sentMessages.contains(pid)) {
            this.sentMessages.remove(pid);
            return true;
        }

        return false;
    }


    @SuppressWarnings({"unchecked"})
    public static final class ServerQuery<I> {
        public static final Object EMPTY_PTR = new Object();
        public static final ScheduledExecutorService TIMER_EXEC = Executors.newSingleThreadScheduledExecutor();
        private final BlockingQueue<Object> syncFlag = new ArrayBlockingQueue<>(1);
        private final String uuid;
        private final Consumer<String> sender;
        private long timeOutMillSeconds;
        private Runnable timeoutHandler;
        private Consumer<I> resultHandler;
        private Consumer<Throwable> errorHandler;


        public ServerQuery(String uuid, Consumer<String> senderAction) {
            this.uuid = uuid;
            this.sender = senderAction;
        }


        public ServerQuery<I> timeout(long mills, Runnable command) {
            this.timeoutHandler = command;
            this.timeOutMillSeconds = mills;
            return this;
        }

        public ServerQuery<I> error(Consumer<Throwable> error) {
            this.errorHandler = error;
            return this;
        }

        public ServerQuery<I> result(Consumer<I> result) {
            this.resultHandler = result;
            return this;
        }

        public void offer(Object result) {
            this.syncFlag.add(result);
        }

        public void sync() {
            this.sender.accept(this.uuid);

            TIMER_EXEC.schedule(() -> {
                this.offer(EMPTY_PTR);
            }, this.timeOutMillSeconds, TimeUnit.MICROSECONDS);

            try {
                var object = this.syncFlag.take();

                if (object == EMPTY_PTR) {
                    this.timeoutHandler.run();
                    return;
                }

                this.resultHandler.accept((I) object);
            } catch (InterruptedException e) {
                if (this.errorHandler != null) {
                    this.errorHandler.accept(e);
                }
            }

        }

        public static final class Holder implements ConnectorListener {
            private final ConcurrentHashMap<String, ServerQuery<?>> lookups = new ConcurrentHashMap<>();

            public void receive(String pid, Object message) {
                var it = this.lookups.entrySet().iterator();

                while (it.hasNext()) {
                    var entry = it.next();
                    var id = entry.getKey();
                    var handler = entry.getValue();

                    if (!Objects.equals(id, pid)) {
                        continue;
                    }

                    handler.offer(message);
                    it.remove();
                    return;
                }
            }

            @Override
            public void messageReceived(RemoteConnector connector, String pid, String channel, String sender, ByteBuf message) {
                this.receive(pid, message);
            }

            public void clear() {
                for (var entry : this.lookups.entrySet()) {
                    entry.getValue().offer(EMPTY_PTR);
                }
            }

            public void register(String uuid, ServerQuery<?> query) {
                this.lookups.put(uuid, query);
            }
        }
    }
}
