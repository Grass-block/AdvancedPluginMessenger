package me.gb2022.apm.remote.example;

import me.gb2022.apm.remote.APMLoggerManager;
import me.gb2022.apm.remote.connector.ProxyConnector;
import me.gb2022.apm.remote.connector.ServerConnector;
import me.gb2022.apm.remote.event.RemoteEventHandler;
import me.gb2022.apm.remote.event.local.ConnectorDisconnectEvent;
import me.gb2022.apm.remote.event.local.ConnectorReadyEvent;
import me.gb2022.apm.remote.event.remote.RemoteMessageEvent;
import me.gb2022.apm.remote.event.remote.RemoteQueryEvent;
import me.gb2022.apm.remote.event.remote.ServerJoinEvent;
import me.gb2022.apm.remote.event.remote.ServerQuitEvent;
import me.gb2022.apm.remote.protocol.BufferUtil;

import java.net.InetSocketAddress;

public interface ExampleApplication {
    InetSocketAddress ADDRESS = new InetSocketAddress("127.0.0.1", 63347);
    byte[] KEY = new byte[]{1, 1, 4, 5, 1, 4, 1, 9, 1, 9, 8, 1, 0};

    static void main(String[] args) {
        APMLoggerManager.useDebugLogger();

        ServerConnector server1 = new ServerConnector("server1", ADDRESS, KEY);
        ServerConnector server2 = new ServerConnector("server2", ADDRESS, KEY);
        ProxyConnector proxy = new ProxyConnector("proxy", ADDRESS, KEY);
        try {
            proxy.addMessageHandler(new ServerListener());
            proxy.start();

            server2.connect();
            server2.waitForReady();
            server1.connect();
            server1.waitForReady();
            proxy.waitForReady();


            server1.getServer("server2").sendMessage("/hello", (buffer) -> {
                BufferUtil.writeString(buffer, "hello!");
            });
            server1.getServer("server2").sendQuery("/query", (buffer) -> {
                        BufferUtil.writeString(buffer, "[server1] server2 are you there?");
                    })
                    .result((buffer) -> System.out.println(BufferUtil.readString(buffer)))
                    .timeout(1145,()-> System.out.println("timeout!"))
                    .sync();
        } finally {
            server1.disconnect();
            server2.disconnect();
            proxy.stop();
        }
    }


    class ServerListener {
        @RemoteEventHandler
        public void onServerJoin(ServerJoinEvent event) {
            System.out.printf("[%s] server %s joined.%n", event.getConnector().getId(), event.getServer().getId());
        }

        @RemoteEventHandler
        public void onServerQuit(ServerQuitEvent event) {
            System.out.printf("[%s] server %s left.%n", event.getConnector().getId(), event.getServer().getId());
        }

        @RemoteEventHandler
        public void onServerStop(ConnectorReadyEvent event) {
            System.out.printf("[%s] connector ready.%n", event.getConnector().getId());
        }

        @RemoteEventHandler
        public void onServerStop(ConnectorDisconnectEvent event) {
            System.out.printf("[%s] connector stopped.%n", event.getConnector().getId());
        }
    }


    class PingPongHandler {
        @RemoteEventHandler("/query")
        public void onMessagePingPong(RemoteQueryEvent message) {
            BufferUtil.writeString(message.getResult(), "[server2] im here :D");
        }

        @RemoteEventHandler("/hello")
        public void onHello(RemoteMessageEvent message) {
            System.out.printf("[%s]%s%n", message.getServer().getId(), BufferUtil.readString(message.getData()));
        }
    }
}
