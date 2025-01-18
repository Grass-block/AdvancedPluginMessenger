package me.gb2022.apm.remote.event;

import me.gb2022.apm.remote.connector.RemoteConnector;

public final class EndpointJoinEvent extends RemoteEvent{
    private final String server;

    public EndpointJoinEvent(RemoteConnector connector, String server) {
        super(connector);
        this.server = server;
    }

    public String getServer() {
        return server;
    }
}
