package me.gb2022.apm.remote.protocol;

import io.netty.channel.ChannelHandlerContext;

public final class Endpoint {
    private final String id;
    private final ChannelHandlerContext context;

    public Endpoint(String id, ChannelHandlerContext context) {
        this.id = id;
        this.context = context;
    }

    public String getId() {
        return id;
    }

    public ChannelHandlerContext getContext() {
        return context;
    }
}
