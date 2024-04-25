package me.gb2022.pluginsmessenger.event;

import io.netty.buffer.ByteBuf;

@FunctionalInterface
public interface PluginQueryHandler {
    void onQuery(String sender, String channel, ByteBuf message, String sessionId);
}
