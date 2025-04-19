package me.gb2022.apm.remote.event;

import io.netty.buffer.ByteBuf;
import me.gb2022.apm.remote.connector.ConnectorListener;
import me.gb2022.apm.remote.connector.RemoteConnector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

public final class DebugListener implements ConnectorListener {
    public static final Logger LOGGER = LogManager.getLogger("APM-debug");

    @Override
    public void messageReceived(RemoteConnector connector, String pid, String channel, String sender, ByteBuf message) {
        LOGGER.info("[msg] ({}) {} {}", pid, channel, sender);
    }

    @Override
    public void serverJoined(RemoteConnector connector, String server) {
        LOGGER.info("[login] {}", server);
    }

    @Override
    public void serverLeft(RemoteConnector connector, String server) {
        LOGGER.info("[logout] {}", server);
    }

    @Override
    public void onMessagePassed(RemoteConnector connector, String pid, String channel, String sender, String receiver, ByteBuf message) {
        LOGGER.info("[pass] ({}) {} {} {}", pid, channel, sender, receiver);
    }

    @Override
    public void connectorReady(RemoteConnector connector) {
        LOGGER.info("[ready] {}", connector);
    }

    @Override
    public void endpointLoginResult(RemoteConnector connector, boolean success, String message, String[] servers) {
        LOGGER.info("[login-result] {} ({}), discovered {}", success, message, Arrays.toString(servers));
    }
}
