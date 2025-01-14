package me.gb2022.apm.client.event;

public abstract class ClientProtocolEvent {
    private final String player;

    protected ClientProtocolEvent(String player) {
        this.player = player;
    }

    public String getPlayer() {
        return player;
    }
}
