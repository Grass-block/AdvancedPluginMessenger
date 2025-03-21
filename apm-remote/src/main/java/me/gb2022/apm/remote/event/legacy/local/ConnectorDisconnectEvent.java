package me.gb2022.apm.remote.event.legacy.local;

import me.gb2022.apm.remote.connector.RemoteConnector;
import me.gb2022.apm.remote.event.legacy.ConnectorEvent;

public class ConnectorDisconnectEvent extends ConnectorEvent {
    public ConnectorDisconnectEvent(RemoteConnector connector) {
        super(connector);
    }
}
