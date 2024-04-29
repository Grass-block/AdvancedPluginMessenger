package me.gb2022.apm.remote.event.remote;

import me.gb2022.apm.remote.Server;
import me.gb2022.apm.remote.connector.RemoteConnector;
import me.gb2022.apm.remote.event.ConnectorEvent;

public abstract class RemoteEvent extends ConnectorEvent {
    private final Server server;

    protected RemoteEvent(RemoteConnector connector,Server sender) {
        super(connector);
        this.server = sender;
    }

    public Server getServer() {
        return server;
    }
}
