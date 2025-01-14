package me.gb2022.apm.client.backend;

import me.gb2022.apm.client.ClientMessageProtocol;
import me.gb2022.apm.client.ClientMessenger;
import me.gb2022.apm.client.event.ClientProtocolInitEvent;
import me.gb2022.apm.client.event.ClientRequestEvent;
import me.gb2022.apm.client.event.driver.ClientEventHandler;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

public abstract class MessageBackend {
    private final Set<String> players = new HashSet<>();

    public static MessageBackend bukkit(Plugin provider) {
        return new BukkitBackend(provider);
    }

    public abstract void sendMessage(String name, String rawMessage);


    public final boolean handleMessage(String message, String playerName) {
        if (!message.startsWith(ClientMessageProtocol.PROTOCOL_PREFIX)) {
            return false;
        }
        if (message.equals(ClientMessageProtocol.CLIENT_PROTOCOL_DETECT)) {
            sendMessage(playerName, ClientMessageProtocol.CLIENT_PROTOCOL_INIT);
            ClientMessenger.EVENT_BUS.callEvent(new ClientProtocolInitEvent(playerName), ClientEventHandler.LISTENER_GLOBAL_EVENT_CHANNEL);
            this.players.add(playerName);
            return true;
        }
        int split = message.indexOf('}');

        String protocol = message.substring(2, split);
        String request = message.substring(split + 1);

        String path;
        String[] args;

        if (request.contains("?")) {
            path = request.split("\\?")[0];
            args = request.split("\\?")[1].split(";");
        } else {
            path = request;
            args = new String[0];
        }


        protocol = protocol.split(":")[1];

        if (protocol.equals("request")) {
            ClientMessenger.EVENT_BUS.callEvent(new ClientRequestEvent(playerName, args, path), path);
        } else {
            ClientMessenger.EVENT_BUS.callEvent(new ClientRequestEvent(playerName, args, path), protocol + ":" + path);
        }

        return true;
    }

    public final boolean isProtocolPlayer(String name) {
        return this.players.contains(name);
    }

    public final void removePlayer(String name) {
        this.players.remove(name);
    }

    public void start() {
    }

    public void stop() {
    }
}
