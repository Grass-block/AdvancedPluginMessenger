package me.gb2022.apm.client;

import me.gb2022.apm.client.backend.MessageBackend;
import me.gb2022.apm.client.event.driver.ClientMessageEventBus;
import me.gb2022.commons.container.ObjectContainer;

public interface ClientMessenger {
    ObjectContainer<MessageBackend> BACKEND = new ObjectContainer<>();
    ClientMessageEventBus EVENT_BUS = new ClientMessageEventBus();

    static MessageBackend getBackend() {
        return BACKEND.get();
    }

    static void setBackend(MessageBackend backend) {
        BACKEND.set(backend);
    }

    static void sendResponse(String player, String path, Object response) {
        String msg = ClientMessageProtocol.CLIENT_RESPONSE_PROTOCOL.formatted(path, response.toString());
        getBackend().sendMessage(player, msg);
    }

    static void sendMessage(String player, String protocol, Object msg) {
        String msg2 = ClientMessageProtocol.MESSAGE_FORMAT.formatted(protocol, msg.toString());
        getBackend().sendMessage(player, msg2);
    }

    static boolean isProtocolPlayer(String player) {
        return getBackend().isProtocolPlayer(player);
    }

    static void connect(String player, String address) {
        sendMessage(player, "connect", address);
    }
}
