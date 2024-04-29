package me.gb2022.apm.remote.event.local;

import me.gb2022.apm.remote.connector.RemoteConnector;
import me.gb2022.apm.remote.event.ConnectorEvent;

public class ConnectorReadyEvent extends ConnectorEvent {
    public ConnectorReadyEvent(RemoteConnector connector) {
        super(connector);
    }
}
