package me.gb2022.apm.remote;

import me.gb2022.apm.remote.connector.ProxyConnector;
import me.gb2022.apm.remote.connector.ServerConnector;
import me.gb2022.apm.remote.protocol.BufferUtil;

import java.net.InetSocketAddress;

public interface Test {
    InetSocketAddress ADDRESS = new InetSocketAddress("127.0.0.1", 63347);
    byte[] KEY = new byte[]{1, 1, 4, 5, 1, 4, 1, 9, 1, 9, 8, 1, 0};

    static void main(String[] args) {
        ServerConnector server1 = new ServerConnector("server1", ADDRESS, KEY);
        ServerConnector server2 = new ServerConnector("server2", ADDRESS, KEY);

        ProxyConnector proxy = new ProxyConnector("proxy", ADDRESS, KEY);
        try {
            proxy.start();

            server2.connect();
            server2.waitForReady();

            server1.connect();
            server1.waitForReady();

            proxy.waitForReady();

            server1.getServer("proxy").sendMessage("channel", (buffer) -> {
                BufferUtil.writeString(buffer, "hello fucking world!");
            });

            server1.getServer("server2").sendMessage("channel", (buffer) -> {
                BufferUtil.writeString(buffer, "say hi to server 02!");
            });
        }finally {
            server1.disconnect();
            server2.disconnect();
            proxy.stop();
        }
    }
}
