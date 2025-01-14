package me.gb2022.apm.remote.event.remote;

import me.gb2022.apm.remote.connector.RemoteConnector;
import me.gb2022.apm.remote.event.ConnectorEvent;

public abstract class RemoteEvent extends ConnectorEvent {
    private final String server;

    protected RemoteEvent(RemoteConnector connector,String sender) {
        super(connector);
        this.server = sender;
    }

    public String getServer() {
        return server;
    }
}
