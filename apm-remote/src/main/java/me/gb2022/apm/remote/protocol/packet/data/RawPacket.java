package me.gb2022.apm.remote.protocol.packet.data;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import me.gb2022.apm.remote.protocol.packet.DeserializedConstructor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.function.Consumer;

public final class RawPacket extends DataPacket {
    private final byte[] message;

    public RawPacket(String channel, String receiver, byte[] message) {
        super(channel, receiver);
        this.message = message;
    }

    public RawPacket(String channel, String receiver, ByteBuf message) {
        super(channel, receiver);
        this.message = new byte[message.readableBytes()];
        message.readBytes(this.message);
        message.release();
    }

    public RawPacket(String channel, String receiver, Consumer<ByteBuf> writer) {
        super(channel, receiver);

        var buffer = ByteBufAllocator.DEFAULT.buffer();

        writer.accept(buffer);

        this.message = new byte[buffer.readableBytes()];

        buffer.readBytes(this.message);
        buffer.release();
    }

    @DeserializedConstructor
    public RawPacket(ByteBuf data) {
        super(data);

        int len = data.readInt();

        this.message = new byte[len];
        data.readBytes(this.message);
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
