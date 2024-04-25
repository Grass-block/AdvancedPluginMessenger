package me.gb2022.pluginsmessenger.event;

import io.netty.buffer.ByteBuf;

@SuppressWarnings("ClassCanBeRecord")
public class RemoteMessage {
    private final ByteBuf message;
    private final String id;

    public RemoteMessage(ByteBuf message, String id) {
        this.message = message;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public ByteBuf getMessage() {
        return message;
    }
}
