package me.gb2022.apm.remote.event;

import me.gb2022.apm.remote.connector.RemoteConnector;

public abstract class RemoteEvent {
    protected final RemoteConnector connector;

    public RemoteEvent(RemoteConnector connector) {
        this.connector = connector;
    }

    public RemoteConnector getConnector() {
        return connector;
    }
}
