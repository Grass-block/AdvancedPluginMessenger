package me.gb2022.apm.remote.protocol.message;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import me.gb2022.apm.remote.util.BufferUtil;

public class ServerMessage extends Message {
    private final MessageType dataType;
    private final String sender;
    private String receiver;
    private final String channel;
    private final String uuid;
    private final ByteBuf data;

    public ServerMessage(MessageType dataType, String sender, String receiver, String channel, String uuid, ByteBuf data) {
        this.dataType = dataType;
        this.sender = sender;
        this.receiver = receiver;
        this.channel = channel;
        this.uuid = uuid;

        this.data = data.copy();
    }

    public ServerMessage(MessageType type, ByteBuf data) {
        this.dataType = type;

        this.sender = BufferUtil.readString(data);
        this.receiver = BufferUtil.readString(data);
        this.channel = BufferUtil.readString(data);
        this.uuid = BufferUtil.readString(data);

        this.data = ByteBufAllocator.DEFAULT.ioBuffer();
        this.data.writeBytes(BufferUtil.readArray(data));
    }


    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getChannel() {
        return channel;
    }

    public ByteBuf getData() {
        return data;
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public void write(ByteBuf data) {
        BufferUtil.writeString(data, this.sender);
        BufferUtil.writeString(data, this.receiver);
        BufferUtil.writeString(data, this.channel);
        BufferUtil.writeString(data, this.uuid);

        data.writeInt(this.data.writerIndex());
        data.writeBytes(this.data);
    }

    @Override
    public MessageType getType() {
        return this.dataType;
    }

    @Override
    public String toString() {
        return "ServerMessage{flow=%s->%s,ch=%s,id=%s,data=%s}".formatted(this.sender, this.receiver, this.channel, this.uuid, this.data.writerIndex());
    }
}
