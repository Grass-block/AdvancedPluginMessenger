package me.gb2022.apm.remote.connector;

import me.gb2022.apm.remote.SOUTLogger;
import me.gb2022.apm.remote.object.Server;
import me.gb2022.apm.remote.protocol.message.ServerMessage;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

public abstract class RemoteConnector {
    protected final Logger logger;

    private final HashMap<String, Server> servers = new HashMap<>();
    private final InetSocketAddress binding;
    private final String id;
    private boolean ready = false;

    protected RemoteConnector(InetSocketAddress binding, String id) {
        this.binding = binding;
        this.id = id;
        this.logger=new SOUTLogger("APM/"+this.getClass().getSimpleName()+"[%s]".formatted(id));
    }

    public InetSocketAddress getBinding() {
        return binding;
    }

    public Server getServer(String id) {
        return this.servers.get(id);
    }

    public void addServer(String id, Server server) {
        this.servers.put(id, server);
    }

    public void removeServer(String id) {
        this.servers.remove(id);
    }

    public String getId() {
        return this.id;
    }

    public Map<String, Server> getServers() {
        return this.servers;
    }

    public void waitForReady() {
        while (!this.ready) {
            Thread.yield();
        }
    }

    void ready() {
        this.ready = true;
    }

    public abstract void sendMessage(ServerMessage message);

    public Set<String> getServerInGroup() {
        Set<String> set = new HashSet<>(getServers().keySet());
        set.add(this.getId());
        return set;
    }

    public void handleMessage(ServerMessage message, Consumer<ServerMessage> notMatch){
        if(!Objects.equals(message.getReceiver(), this.id)){
            notMatch.accept(message);
            return;
        }
        this.logger.info(message.toString());//todo:handle message
    }
}
