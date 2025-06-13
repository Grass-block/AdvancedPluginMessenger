package me.gb2022.apm.remote;

import me.gb2022.apm.remote.event.APMRemoteEvent;
import me.gb2022.apm.remote.event.channel.ChannelListener;
import me.gb2022.apm.remote.event.channel.MessageChannel;
import me.gb2022.apm.remote.event.message.RemoteMessageEvent;
import me.gb2022.apm.remote.event.message.RemoteMessageSurpassEvent;
import me.gb2022.apm.remote.event.message.RemoteQueryEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.Random;

public interface ExampleApplication {
    InetSocketAddress ADDRESS = new InetSocketAddress("127.0.0.1", 63345);
    byte[] KEY = new byte[]{1, 1, 4, 5, 1, 4, 1, 9, 1, 9, 8, 1, 0};

    static void main(String[] args) throws Exception {
        var proxy = new RemoteMessenger(true, "proxy", ADDRESS, KEY);
        proxy.connector().waitForReady();

        var server1 = new RemoteMessenger(false, "server1", ADDRESS, KEY);
        var server2 = new RemoteMessenger(false, "server2", ADDRESS, KEY);
        var server3 = new RemoteMessenger(false, "server3", ADDRESS, KEY);

        server2.registerEventHandler(ExampleStaticListener.class);
        server1.registerEventHandler(new ExampleObjectListener());
        server2.registerEventHandler(new QueryListener());

        server1.connector().waitForReady();
        server2.connector().waitForReady();
        server3.connector().waitForReady();

        //proxy.message("server2", "/handler_test", "Hi!");
        //proxy.message("server1", "/handler_test", "Hi!");

        var data = generate128MBRandomBytes();
        long last = System.currentTimeMillis();


        var c_proxy = proxy.messageChannel("/test1");
        var c_server1 = server1.messageChannel("/test1");
        var c_server2 = server2.messageChannel("/test1");
        var c_server3 = server3.messageChannel("/test1");

        c_proxy.setListener(new ChannelListener() {
            @Override
            public void handle(MessageChannel channel, RemoteMessageEvent event) {
                System.out.println("[proxy] message: " + event.decode(String.class));
            }

            @Override
            public void handle(MessageChannel channel, RemoteMessageSurpassEvent event) {
                System.out.println("[proxy] surpass: " + event.decode(String.class));
            }
        });

        c_server1.setListener(new ChannelListener() {
            @Override
            public void handle(MessageChannel channel, RemoteMessageEvent event) {
                System.out.println("[server1] message: " + event.decode(String.class));
            }
        });

        c_server2.setListener(new ChannelListener() {
            @Override
            public void handle(MessageChannel channel, RemoteMessageEvent event) {
                System.out.println("[server2] message: " + event.decode(String.class));
            }
        });

        c_server3.setListener(new ChannelListener() {
            @Override
            public void handle(MessageChannel channel, RemoteMessageEvent event) {
                System.out.println("[server3] message: " + event.decode(String.class));
            }
        });


        c_server1.message("server2", "MESSAGE server1 -> server2");
        c_server1.broadcast("MESSAGE server1 -> all");

        Thread.sleep(100);

        c_server1.message("proxy", "MESSAGE server1 -> proxy");
        c_proxy.message("server2", "MESSAGE proxy -> server2");

        Thread.sleep(100);
        c_proxy.broadcast("MESSAGE proxy -> all");


        /*

        server1.messageChannel("/huge").setListener(new ChannelListener() {
            @Override
            public void handle(MessageChannel channel, RemoteMessageEvent event) {
                System.out.println("received");

                var array = new byte[data.length];

                event.message().readBytes(array);

                if (Arrays.equals(array, data)) {
                    System.out.println(System.currentTimeMillis() - last);
                }
            }
        });


        server1.message("server2", "/huge", (b) -> b.writeBytes(data));

        server1.broadcast("/broadcast", "Hey guys, i'm server1!");
        server1.query("server2", "music:get", "server2, are you there?")
                .result((result) -> System.out.println("[query]query result from server2: " + result))
                .timeout(1000, () -> System.out.println("[query]timeout query from server2"))
                .error(Throwable::printStackTrace)
                .request();

         */
    }

    static byte[] generate128MBRandomBytes() {
        int size = 24 * 1024 * 1024; // 128 MB
        byte[] data = new byte[size];
        new Random().nextBytes(data); // 填充随机字节
        return data;
    }

    interface ExampleStaticListener {
        Logger LOGGER = LogManager.getLogger("StaticListener");

        @APMRemoteEvent
        static void onConnectorReady(RemoteMessenger context, me.gb2022.apm.remote.event.connector.ConnectorReadyEvent event) {
            LOGGER.info("[{}] connector ready", context.getIdentifier());
        }

        @APMRemoteEvent("/handler_test")
        static void onRemoteMessage(RemoteMessenger context, RemoteMessageEvent event) {
            LOGGER.info("[{}] remote message: {}", context.getIdentifier(), event.decode(String.class));
        }
    }

    class QueryListener {
        @APMRemoteEvent("music:get")
        public void onRemoteQuery(RemoteMessenger context, RemoteQueryEvent event) {
            System.out.println(event.decode(String.class));
            event.write("Bro im here!");
        }
    }

    class ExampleObjectListener {
        public static final Logger LOGGER = LogManager.getLogger("ObjectListener");

        @APMRemoteEvent
        public void onConnectorReady(RemoteMessenger context, me.gb2022.apm.remote.event.connector.ConnectorReadyEvent event) {
            LOGGER.info("[{}] connector ready", context.getIdentifier());
        }

        @APMRemoteEvent("/handler_test")
        public void onRemoteMessage(RemoteMessenger context, RemoteMessageEvent event) {
            LOGGER.info("[{}] remote message: {}", context.getIdentifier(), event.decode(String.class));
        }
    }
}
