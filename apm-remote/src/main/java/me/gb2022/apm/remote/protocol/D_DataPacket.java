package me.gb2022.apm.remote.protocol;

import io.netty.buffer.ByteBuf;
import me.gb2022.simpnet.packet.DeserializedConstructor;
import me.gb2022.simpnet.packet.Packet;
import me.gb2022.simpnet.util.BufferUtil;

public abstract class D_DataPacket implements Packet {
    private final String channel;
    private String sender;
    private String uuid;
    private String receiver;

    public D_DataPacket(String sender, String receiver, String channel, String uuid) {
        this.sender = sender;
        this.receiver = receiver;
        this.channel = channel;
        this.uuid = uuid;
    }

    public D_DataPacket(String channel, String receiver) {
        this.channel = channel;
        this.receiver = receiver;
    }

    @DeserializedConstructor
    public D_DataPacket(ByteBuf data) {
        this.sender = BufferUtil.readString(data);
        this.receiver = BufferUtil.readString(data);
        this.channel = BufferUtil.readString(data);
        this.uuid = BufferUtil.readString(data);
    }

    public void fillSenderInformation(String uuid, String sender) {
        this.uuid = uuid;
        this.sender = sender;
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

    public String getUuid() {
        return uuid;
    }

    @Override
    public final void write(ByteBuf data) {
        BufferUtil.writeString(data, this.sender);
        BufferUtil.writeString(data, this.receiver);
        BufferUtil.writeString(data, this.channel);
        BufferUtil.writeString(data, this.uuid);
        this.write0(data);
    }

    public abstract void write0(ByteBuf data);

    @Override
    public String toString() {
        return "Data{flow=%s->%s,ch=%s,id=%s}".formatted(
                this.sender,
                this.receiver,
                this.channel,
                this.uuid
        );
    }
}
