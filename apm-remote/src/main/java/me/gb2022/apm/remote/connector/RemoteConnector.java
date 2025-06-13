package me.gb2022.apm.remote.connector;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import me.gb2022.apm.remote.protocol.APMProtocol;
import me.gb2022.apm.remote.util.BlockedRunnable;
import me.gb2022.apm.remote.protocol.packet.DataPacket;
import me.gb2022.apm.remote.protocol.packet.P20_RawData;
import me.gb2022.simpnet.packet.Packet;
import me.gb2022.simpnet.util.MessageVerification;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;

public abstract class RemoteConnector implements BlockedRunnable {
    public static final String BROADCAST_ID = "_apm://broadcast";
    public static final String BROADCAST_ACCEPT = "_apm://broadcast/accept";

    protected final InetSocketAddress binding;
    protected final String identifier;
    protected final ConnectorListener listener;
    protected final MessageVerification verification;
    protected final byte[] key;

    private final Set<String> sentMessages = new HashSet<>();

    boolean debug;
    boolean ready = false;

    protected RemoteConnector(InetSocketAddress binding, String identifier, byte[] key, ConnectorListener listener) {
        this.verification = new MessageVerification(MessageVerification.Mode.AES_ECB, key, APMProtocol.MAGIC_NUMBER);
        this.binding = binding;
        this.identifier = identifier;
        this.key = key;
        this.listener = listener;
    }

    public abstract Logger getLogger();

    public MessageVerification getVerification() {
        return verification;
    }

    public byte[] getKey() {
        return key;
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

    public final void debug(boolean debug) {
        this.debug = debug;
    }


    public abstract void open();

    public void close() {
        this.ready = false;
    }


    //servers
    public abstract Set<String> getServerInGroup();


    public final void sendPacket(Packet packet, Channel... targets) {
        for (var c : targets) {
            if (c == null) {
                continue;
            }
            c.writeAndFlush(packet);
        }
    }

    public final String sendPacket(DataPacket packet, String uuid) {
        packet.fillSenderInformation(uuid, this.getIdentifier());

        if (this.debug) {
            getLogger().info("[out] {}", packet);
        }

        this.sentMessages.add(uuid);

        if (this instanceof ExchangeConnector && Objects.equals(packet.getReceiver(), BROADCAST_ID)) {
            packet.setReceiver(BROADCAST_ACCEPT);
            for (var id : this.getServerInGroup()) {
                this.sendPacket(packet, getPacketDest(id));
            }
            return uuid;
        }

        this.sendPacket(packet, getPacketDest(packet.getReceiver()));
        return uuid;
    }


    //implementation
    public void handlePacket(Packet packet, Channel channel) {
        if (!(packet instanceof DataPacket dp)) {
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

        if (packet instanceof P20_RawData p) {
            var buffer = ByteBufAllocator.DEFAULT.buffer();
            buffer.writeBytes(p.getMessage());

            this.listener.messageReceived(this, p.getUuid(), p.getChannel(), p.getSender(), buffer);
            buffer.release();
            return;
        }

        getLogger().warn("[{}] unhandled data packet: {}", this.identifier, packet);
    }

    public abstract Channel getPacketDest(String receiver);

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
