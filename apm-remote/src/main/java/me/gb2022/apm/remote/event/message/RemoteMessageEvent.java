package me.gb2022.apm.remote.event.message;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.codec.ObjectCodec;
import me.gb2022.apm.remote.connector.RemoteConnector;
import me.gb2022.apm.remote.event.RemoteEvent;

public class RemoteMessageEvent extends RemoteEvent {
    private final String sender;
    private final String uuid;
    private final String channel;
    private final ByteBuf message;

    public RemoteMessageEvent(RemoteConnector connector, String sender, String uuid, String channel, ByteBuf message) {
        super(connector);
        this.sender = sender;
        this.uuid = uuid;
        this.channel = channel;
        this.message = message;
    }

    public ByteBuf message() {
        return this.message.readerIndex(0);
    }

    public String sender() {
        return this.sender;
    }

    public String uuid() {
        return this.uuid;
    }

    public String channel() {
        return this.channel;
    }

    public <I> I decode(Class<I> type){
        return ObjectCodec.decode(message(), type);
    }
}
