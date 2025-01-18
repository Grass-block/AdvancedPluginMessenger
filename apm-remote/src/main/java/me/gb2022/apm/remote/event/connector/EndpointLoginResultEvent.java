package me.gb2022.apm.remote.event.connector;

import me.gb2022.apm.remote.connector.RemoteConnector;
import me.gb2022.apm.remote.event.RemoteEvent;

public final class EndpointLoginResultEvent extends RemoteEvent {
    private final boolean success;
    private final String message;
    private final String[] servers;

    public EndpointLoginResultEvent(RemoteConnector connector, boolean success, String message, String[] servers) {
        super(connector);
        this.success = success;
        this.message = message;
        this.servers = servers;
    }


    public boolean success() {
        return success;
    }

    public String message() {
        return message;
    }

    public String[] servers() {
        return servers;
    }
}
