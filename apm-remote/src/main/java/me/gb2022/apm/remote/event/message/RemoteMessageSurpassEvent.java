package me.gb2022.apm.remote.event.message;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.connector.RemoteConnector;

import java.util.function.Consumer;

public final class RemoteMessageSurpassEvent extends RemoteQueryEvent {
    public RemoteMessageSurpassEvent(RemoteConnector connector, String sender, String uuid, String channel, ByteBuf message) {
        super(connector, sender, uuid, channel, message);
    }

    public ByteBuf examine() {
        if (hasResult()) {
            return this.result;
        }

        return this.result.writeBytes(this.message());
    }

    @Override
    public void result(Consumer<ByteBuf> reader) {
        var buffer = this.examine();
        reader.accept(buffer);
        buffer.release();
    }
}
