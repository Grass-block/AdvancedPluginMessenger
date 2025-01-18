package me.gb2022.apm.remote.event.connector;

import me.gb2022.apm.remote.connector.RemoteConnector;
import me.gb2022.apm.remote.event.RemoteEvent;

import java.net.InetSocketAddress;

public final class ConnectorStartEvent extends RemoteEvent {
    private final InetSocketAddress target;
    private final String username;
    private final byte[] password;
    private final boolean proxy;

    public ConnectorStartEvent(RemoteConnector connector, InetSocketAddress target, String username, byte[] password, boolean proxy) {
        super(connector);
        this.target = target;
        this.username = username;
        this.password = password;
        this.proxy = proxy;
    }


    public InetSocketAddress target() {
        return target;
    }

    public String username() {
        return username;
    }

    public byte[] password() {
        return password;
    }

    public boolean proxy() {
        return proxy;
    }
}
