package me.gb2022.apm.remote.event;

import me.gb2022.apm.remote.connector.RemoteConnector;

public final class EndpointLeftEvent extends RemoteEvent{
    private final String server;

    public EndpointLeftEvent(RemoteConnector connector, String server) {
        super(connector);
        this.server = server;
    }

    public String getServer() {
        return server;
    }
}
