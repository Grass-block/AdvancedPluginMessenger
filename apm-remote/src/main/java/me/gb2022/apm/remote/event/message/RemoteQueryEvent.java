package me.gb2022.apm.remote.event.message;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import me.gb2022.simpnet.codec.ObjectCodec;
import me.gb2022.apm.remote.connector.RemoteConnector;

import java.util.function.Consumer;

public class RemoteQueryEvent extends RemoteMessageEvent {
    public ByteBuf result = ByteBufAllocator.DEFAULT.buffer();

    public RemoteQueryEvent(RemoteConnector connector, String sender, String uuid, String channel, ByteBuf message) {
        super(connector, sender, uuid, channel, message);
    }

    public ByteBuf result() {
        return this.result;
    }

    public void write(Consumer<ByteBuf> writer) {
        writer.accept(this.result);
    }

    public <I> void write(I object) {
        this.write((buffer) -> ObjectCodec.encode(buffer, object));
    }

    public boolean hasResult() {
        return this.result.writerIndex() > 0;
    }

    public void result(Consumer<ByteBuf> reader) {
        reader.accept(this.result);
        //this.result.release();
    }
}
