package me.gb2022.apm.remote.event.local;

import me.gb2022.apm.remote.connector.RemoteConnector;
import me.gb2022.apm.remote.event.ConnectorEvent;

public class ConnectorDisconnectEvent extends ConnectorEvent {
    public ConnectorDisconnectEvent(RemoteConnector connector) {
        super(connector);
    }
}
