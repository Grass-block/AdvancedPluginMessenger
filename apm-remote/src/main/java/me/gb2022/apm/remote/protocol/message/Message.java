package me.gb2022.apm.remote.protocol.message;

import io.netty.buffer.ByteBuf;

public abstract class Message {
    public abstract void write(ByteBuf data);

    public abstract EnumMessages getType();

    public void writeData(ByteBuf buffer) {
        buffer.writeByte(this.getType().id);
        this.write(buffer);
    }
}
