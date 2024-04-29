package me.gb2022.apm.remote;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import me.gb2022.apm.remote.connector.RemoteConnector;
import me.gb2022.apm.remote.protocol.MessageType;
import me.gb2022.apm.remote.protocol.message.ServerMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class Server {
    public static final String BROADCAST_ID = "__broadcast__";

    private final String id;
    private final RemoteConnector parent;

    private final Map<String, ByteBuf> cache = new HashMap<>();

    public Server(String id, RemoteConnector parent) {
        this.id = id;
        this.parent = parent;
    }

    public void sendMessage(String channel, Consumer<ByteBuf> msg) {
        ByteBuf message = ByteBufAllocator.DEFAULT.ioBuffer();
        msg.accept(message);
        String uuid = UUID.randomUUID().toString();
        this.sendMessage(new ServerMessage(MessageType.MESSAGE, this.parent.getId(), this.id, channel, uuid, message));
    }

    public ServerQuery sendQuery(String channel, Consumer<ByteBuf> msg) {
        ByteBuf message = ByteBufAllocator.DEFAULT.ioBuffer();
        msg.accept(message);
        String uuid = UUID.randomUUID().toString();
        this.sendMessage(new ServerMessage(MessageType.QUERY, this.parent.getId(), this.id, channel, uuid, message));
        return new ServerQuery(this, uuid);
    }

    public void sendBroadcast(String channel, Consumer<ByteBuf> msg) {
        ByteBuf message = ByteBufAllocator.DEFAULT.ioBuffer();
        msg.accept(message);
        String uuid = UUID.randomUUID().toString();
        this.sendMessage(new ServerMessage(MessageType.MESSAGE, this.parent.getId(), BROADCAST_ID, channel, uuid, message));
    }

    public void handleRemoteQueryResult(String uuid, ByteBuf msg) {
        this.cache.put(uuid, msg);
    }

    public void sendMessage(ServerMessage message) {
        this.parent.sendMessage(message);
    }

    public String getId() {
        return id;
    }


    public static final class ServerQuery {
        private final Server provider;
        private final String uuid;
        private Runnable timeOut;
        private Consumer<ByteBuf> result;
        private long timeOutMillSeconds;

        public ServerQuery(Server provider, String uuid) {
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
