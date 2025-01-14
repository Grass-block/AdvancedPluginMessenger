package me.gb2022.apm.remote.listen;

import io.netty.buffer.ByteBuf;

public interface ChannelListener {
    default void receiveMessage(MessageChannel channel, String pid, String sender, ByteBuf message) {

    }

    default void receiveMessage(MessageChannel channel, String pid, String sender, String message) {

    }
}
