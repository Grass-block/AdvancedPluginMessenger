package me.gb2022.apm.remote.event.legacy.local;

import me.gb2022.apm.remote.connector.RemoteConnector;
import me.gb2022.apm.remote.event.legacy.ConnectorEvent;

public class ConnectorReadyEvent extends ConnectorEvent {
    public ConnectorReadyEvent(RemoteConnector connector) {
        super(connector);
    }
}
