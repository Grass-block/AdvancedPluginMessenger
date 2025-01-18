package me.gb2022.apm.remote.legacy.local;

import me.gb2022.apm.remote.connector.RemoteConnector;
import me.gb2022.apm.remote.legacy.ConnectorEvent;

public class ClientConnectorLoginFailedEvent extends ConnectorEvent {
    public ClientConnectorLoginFailedEvent(RemoteConnector connector) {
        super(connector);
    }
}
