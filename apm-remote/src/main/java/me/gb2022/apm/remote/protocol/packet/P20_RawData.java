package me.gb2022.apm.remote.protocol.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import me.gb2022.simpnet.packet.DeserializedConstructor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.function.Consumer;

public final class P20_RawData extends DataPacket {
    private byte[] message;

    public P20_RawData(String channel, String receiver, byte[] message) {
        super(channel, receiver);
        this.message = message;
    }

    public P20_RawData(String channel, String receiver, ByteBuf message) {
        super(channel, receiver);
        this.message = new byte[message.readableBytes()];
        message.readBytes(this.message);
        message.release();
    }

    public P20_RawData(String channel, String receiver, Consumer<ByteBuf> writer) {
        super(channel, receiver);

        var buffer = ByteBufAllocator.DEFAULT.buffer();

        writer.accept(buffer);

        this.message = new byte[buffer.readableBytes()];

        buffer.readBytes(this.message);
        buffer.release();
    }

    @DeserializedConstructor
    public P20_RawData(ByteBuf data) {
        super(data);

        int len = data.readInt();

        this.message = new byte[len];
        data.readBytes(this.message);
    }

    public void setMessage(ByteBuf message) {
        this.message = new byte[message.writerIndex()];
        message.readBytes(this.message);
    }

    @Override
    public void write0(ByteBuf data) {
        data.writeInt(this.message.length);
        data.writeBytes(this.message);
    }

    public byte[] getMessage() {
        return message;
    }

    public InputStream read() {
        return new ByteArrayInputStream(this.message);
    }
}
