package me.gb2022.apm.remote.object;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import me.gb2022.apm.remote.connector.RemoteConnector;
import me.gb2022.apm.remote.protocol.message.EnumMessages;
import me.gb2022.apm.remote.protocol.message.Message;
import me.gb2022.apm.remote.protocol.message.ServerMessage;

import java.util.function.Consumer;

public class Server {
    public static final String BROADCAST_ID = "__broadcast__";

    private final String id;
    private final RemoteConnector parent;

    public Server(String id, RemoteConnector parent) {
        this.id = id;
        this.parent = parent;
    }

    public void sendMessage(String channel, Consumer<ByteBuf> msg) {
        ByteBuf message = ByteBufAllocator.DEFAULT.ioBuffer();
        msg.accept(message);
        this.sendMessage(new ServerMessage(EnumMessages.MESSAGE, this.parent.getId(), this.id, channel, message));
    }

    public ByteBuf sendQuery(String channel, Consumer<ByteBuf> msg) {
        ByteBuf message = ByteBufAllocator.DEFAULT.ioBuffer();
        msg.accept(message);
        this.sendMessage(new ServerMessage(EnumMessages.QUERY, this.parent.getId(), this.id, channel, message));
        return null;
    }

    public void sendBroadcast(String channel, Consumer<ByteBuf> msg) {
        ByteBuf message = ByteBufAllocator.DEFAULT.ioBuffer();
        msg.accept(message);
        this.sendMessage(new ServerMessage(EnumMessages.MESSAGE, this.parent.getId(), BROADCAST_ID, channel, message));
    }

    public void sendMessage(ServerMessage message) {
        this.parent.sendMessage(message);
    }
}
