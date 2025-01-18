package me.gb2022.apm.remote.event.channel;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.RemoteMessenger;
import me.gb2022.apm.remote.RemoteQuery;
import me.gb2022.apm.remote.event.RemoteEventListener;
import me.gb2022.apm.remote.event.message.RemoteMessageEvent;
import me.gb2022.apm.remote.event.message.RemoteMessageSurpassEvent;
import me.gb2022.apm.remote.event.message.RemoteQueryEvent;

import java.util.Objects;
import java.util.function.Consumer;

public final class MessageChannel implements RemoteEventListener {
    private final String channel;
    private final RemoteMessenger connector;
    private ChannelListener listener;

    public MessageChannel(RemoteMessenger connector, String channel) {
        this.connector = connector;
        this.channel = channel;
    }

    public String message(String target, Consumer<ByteBuf> writer) {
        return this.connector.message(target, this.channel, writer);
    }

    public String broadcast(Consumer<ByteBuf> writer) {
        return this.connector.broadcast(this.channel, writer);
    }

    public RemoteQuery<ByteBuf> query(String target, Consumer<ByteBuf> writer) {
        return this.connector.query(target, this.channel, writer);
    }

    public <I> String message(String target, I object) {
        return this.connector.message(target, this.channel, object);
    }

    public <I> String broadcast(I object) {
        return this.connector.broadcast(this.channel, object);
    }

    public <I> RemoteQuery<I> query(String target, I msg) {
        return this.connector.query(target, this.channel, msg);
    }


    public ChannelListener getListener() {
        return listener;
    }

    public void setListener(ChannelListener listener) {
        this.listener = listener;
    }

    public RemoteMessenger getConnector() {
        return connector;
    }


    @Override
    public void messageSurpassed(RemoteMessenger messenger, RemoteMessageSurpassEvent event) {
        if (!Objects.equals(this.channel, event.channel())) {
            return;
        }
        if (this.listener == null) {
            return;
        }

        this.listener.handle(this, event);
    }

    @Override
    public void remoteQuery(RemoteMessenger messenger, RemoteQueryEvent event) {
        if (!Objects.equals(this.channel, event.channel())) {
            return;
        }
        if (this.listener == null) {
            return;
        }

        this.listener.handle(this, event);
    }

    @Override
    public void remoteMessage(RemoteMessenger messenger, RemoteMessageEvent event) {
        if (!Objects.equals(this.channel, event.channel())) {
            return;
        }
        if (this.listener == null) {
            return;
        }

        this.listener.handle(this, event);
    }
}
