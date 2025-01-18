package me.gb2022.apm.remote.event.connector;

import me.gb2022.apm.remote.connector.RemoteConnector;
import me.gb2022.apm.remote.event.RemoteEvent;

public final class ConnectorReadyEvent extends RemoteEvent {
    public ConnectorReadyEvent(RemoteConnector connector) {
        super(connector);
    }


}
