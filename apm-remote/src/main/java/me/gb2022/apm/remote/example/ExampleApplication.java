package me.gb2022.apm.remote.example;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.RemoteMessenger;
import me.gb2022.apm.remote.connector.RemoteConnector;
import me.gb2022.apm.remote.event.RemoteEventHandler;
import me.gb2022.apm.remote.event.local.ConnectorDisconnectEvent;
import me.gb2022.apm.remote.event.local.ConnectorReadyEvent;
import me.gb2022.apm.remote.event.remote.*;
import me.gb2022.apm.remote.listen.ChannelListener;
import me.gb2022.apm.remote.listen.ConnectorListener;
import me.gb2022.apm.remote.listen.MessageChannel;
import me.gb2022.apm.remote.util.BufferUtil;

import java.net.InetSocketAddress;

public interface ExampleApplication {
    InetSocketAddress ADDRESS = new InetSocketAddress("127.0.0.1", 63345);
    byte[] KEY = new byte[]{1, 1, 4, 5, 1, 4, 1, 9, 1, 9, 8, 1, 0};

    static void main(String[] args) throws InterruptedException {
        var proxy = new RemoteMessenger(true, "proxy", ADDRESS, KEY);
        var server1 = new RemoteMessenger(false, "server1", ADDRESS, KEY);
        var server2 = new RemoteMessenger(false, "server2", ADDRESS, KEY);

        proxy.addMessageHandler(new ServerListener());
        server2.addMessageHandler(new ExampleApplication.PingPongHandler());

        proxy.messageChannel("/hello").setListener(new ChannelListener() {
            @Override
            public void receiveMessage(MessageChannel channel, String pid, String sender, String message) {
                System.out.println("[proxy] received message: " + message);
            }

            @Override
            public void receiveMessage(MessageChannel channel, String pid, String sender, ByteBuf message) {
                System.out.println("[proxy] received message: " + BufferUtil.readString(message));
            }
        });
        server1.eventChannel().addListener(new ConnectorListener() {
            @Override
            public void connectorReady(RemoteConnector connector) {
                connector.sendMessage("/hello", "proxy", "Hello from server1");
            }

            @Override
            public void messageReceived(RemoteConnector connector, String pid, String channel, String sender, ByteBuf message) {
                System.out.println("[server1] received message: " + BufferUtil.readString(message));
            }

            @Override
            public void messageReceived(RemoteConnector connector, String pid, String channel, String sender, String message) {
                System.out.println("[server1] received message: " + message);
            }
        });
        server2.eventChannel().addListener(new ConnectorListener() {
            @Override
            public void connectorReady(RemoteConnector connector) {
                connector.sendMessage("/hello", "proxy", "Hello from server2");
            }

            @Override
            public void messageReceived(RemoteConnector connector, String pid, String channel, String sender, ByteBuf message) {
                System.out.println("[server2] received message: " + BufferUtil.readString(message));
            }

            @Override
            public void messageReceived(RemoteConnector connector, String pid, String channel, String sender, String message) {
                System.out.println("[server2] received message: " + message);
            }
        });

        server1.getConnector().waitForReady();
        server2.getConnector().waitForReady();

        proxy.sendMessage("server1", "/hello", (buffer) -> {
            BufferUtil.writeString(buffer, "Hello from proxy");
        });

        server1.sendQuery("server2", "/query", (b) -> BufferUtil.writeString(b, "Query request"))
                .result((b) -> System.out.println(BufferUtil.readString(b)))
                .timeout(1000, () -> {})
                .sync();

        server1.sendBroadcast("/hello","Global broadcast");

        Thread.sleep(1000);
    }


    class ServerListener {
        @RemoteEventHandler
        public void onServerJoin(ServerJoinEvent event) {
            System.out.printf("[%s] server %s joined.%n", event.getConnector().getIdentifier(), event.getServer());
        }

        @RemoteEventHandler
        public void onServerQuit(ServerQuitEvent event) {
            System.out.printf("[%s] server %s left.%n", event.getConnector().getIdentifier(), event.getServer());
        }

        @RemoteEventHandler
        public void onServerStop(ConnectorReadyEvent event) {
            System.out.printf("[%s] connector ready.%n", event.getConnector().getIdentifier());
        }

        @RemoteEventHandler
        public void onServerStop(ConnectorDisconnectEvent event) {
            System.out.printf("[%s] connector stopped.%n", event.getConnector().getIdentifier());
        }

        @RemoteEventHandler("/hello")
        public void onMessage(RemoteMessageExchangeEvent event) {
            event.writeResult((b) -> BufferUtil.writeString(b, "_modified"));
        }
    }


    class PingPongHandler {
        @RemoteEventHandler("/query")
        public void onMessagePingPong(RemoteQueryEvent message) {
            message.writeResult((buffer) -> BufferUtil.writeString(buffer, "Query result from "+message.getConnector().getIdentifier()));
        }
    }
}
