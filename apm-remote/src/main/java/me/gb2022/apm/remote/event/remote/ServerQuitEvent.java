package me.gb2022.apm.remote.event.remote;

import me.gb2022.apm.remote.Server;
import me.gb2022.apm.remote.connector.RemoteConnector;

public final class ServerQuitEvent extends RemoteEvent {
    public ServerQuitEvent(RemoteConnector connector, Server sender) {
        super(connector, sender);
    }
}
