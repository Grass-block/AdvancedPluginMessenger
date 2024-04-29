package me.gb2022.apm.remote.protocol.message;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.protocol.MessageType;

public abstract class Message {
    public abstract void write(ByteBuf data);

    public abstract MessageType getType();

    public void writeData(ByteBuf buffer) {
        buffer.writeByte(this.getType().id);
        this.write(buffer);
    }
}
