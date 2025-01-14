package me.gb2022.apm.remote.listen;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.connector.RemoteConnector;
import me.gb2022.apm.remote.protocol.packet.data.RawPacket;

import java.util.Objects;
import java.util.function.Consumer;

public final class MessageChannel implements ConnectorListener {
    private final String channel;
    private RemoteConnector connector;
    private ChannelListener listener;

    public MessageChannel(RemoteConnector connector, String channel) {
        this.connector = connector;
        this.channel = channel;
    }

    public void _setContext(RemoteConnector connector) {
        this.connector = connector;
    }

    public void send(String receiver, Consumer<ByteBuf> writer) {
        this.connector.sendMessage(this.channel, receiver, writer);
    }

    public void send(String receiver, ByteBuf message) {
        this.connector.sendMessage(this.channel, receiver, message);
    }

    public void send(String receiver, String message) {
        this.connector.sendMessage(this.channel, receiver, message);
    }

    public void setListener(ChannelListener listener) {
        this.listener = listener;
    }

    public ChannelListener getListener() {
        return listener;
    }

    @Override
    public void messageReceived(RemoteConnector connector, String pid, String channel, String sender, ByteBuf message) {
        if (!Objects.equals(channel, this.channel)) {
            return;
        }
        this.listener.receiveMessage(this, pid, channel, message);
    }

    @Override
    public void messageReceived(RemoteConnector connector, String pid, String channel, String sender, String message) {
        if (!Objects.equals(channel, this.channel)) {
            return;
        }
        this.listener.receiveMessage(this, pid, channel, message);
    }
}
