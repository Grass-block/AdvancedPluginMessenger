package me.gb2022.apm.remote.event;

import me.gb2022.apm.remote.connector.RemoteConnector;

public abstract class ConnectorEvent {
    private final RemoteConnector connector;

    protected ConnectorEvent(RemoteConnector connector) {
        this.connector = connector;
    }

    public RemoteConnector getConnector() {
        return connector;
    }
}
