package me.gb2022.apm.remote.event.channel;

import me.gb2022.apm.remote.event.message.RemoteMessageEvent;
import me.gb2022.apm.remote.event.message.RemoteMessageSurpassEvent;
import me.gb2022.apm.remote.event.message.RemoteQueryEvent;

public interface ChannelListener {
    default void handle(MessageChannel channel, RemoteMessageEvent event) {
    }

    default void handle(MessageChannel channel, RemoteQueryEvent event) {
    }

    default void handle(MessageChannel channel, RemoteMessageSurpassEvent event) {
    }
}
