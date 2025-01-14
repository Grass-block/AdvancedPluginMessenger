package me.gb2022.apm.client.event;

import me.gb2022.apm.client.ClientMessenger;

public class ClientRequestEvent extends ClientProtocolEvent {
    private final String[] args;
    private final String path;

    public ClientRequestEvent(String player, String[] args, String path) {
        super(player);

        this.args = args;
        this.path = path;
    }

    public String[] getArgs() {
        return args;
    }

    public String getPath() {
        return path;
    }

    public void makeResponse(Object msg) {
        ClientMessenger.sendResponse(getPlayer(), getPath(), msg);
    }
}
