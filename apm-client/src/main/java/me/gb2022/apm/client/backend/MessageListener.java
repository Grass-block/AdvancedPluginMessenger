package me.gb2022.apm.client.backend;

@FunctionalInterface
public interface MessageListener {
    void handleMessage(String playerName, String protocol, String request, String[] args);
}
