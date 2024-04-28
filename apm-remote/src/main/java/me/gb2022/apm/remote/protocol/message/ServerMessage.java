package me.gb2022.apm.remote.protocol.message;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import me.gb2022.apm.remote.protocol.BufferUtil;

public class ServerMessage extends Message {
    private final EnumMessages dataType;
    private final String sender;
    private final String receiver;
    private final String channel;
    private final ByteBuf data;

    public ServerMessage(EnumMessages dataType, String sender, String receiver, String channel, ByteBuf data) {
        this.dataType = dataType;
        this.sender = sender;
        this.receiver = receiver;
        this.channel = channel;
        this.data = data;
    }

    public ServerMessage(EnumMessages type, ByteBuf data) {
        this.dataType = type;

        this.sender = BufferUtil.readString(data);
        this.receiver = BufferUtil.readString(data);
        this.channel = BufferUtil.readString(data);

        this.data = ByteBufAllocator.DEFAULT.ioBuffer();
        this.data.writeBytes(BufferUtil.readArray(data));
    }


    public EnumMessages getDataType() {
        return dataType;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getChannel() {
        return channel;
    }

    public ByteBuf getData() {
        return data;
    }

    @Override
    public void write(ByteBuf data) {
        BufferUtil.writeString(data, this.sender);
        BufferUtil.writeString(data, this.receiver);
        BufferUtil.writeString(data, this.channel);

        data.writeInt(this.data.writerIndex());
        data.writeBytes(this.data);
    }

    @Override
    public EnumMessages getType() {
        return this.dataType;
    }

    @Override
    public String toString() {
        return "ServerMessage{flow=%s->%s,ch=%s,content=%s}".formatted(this.sender, this.receiver, this.channel, BufferUtil.readString(data));
    }
}
