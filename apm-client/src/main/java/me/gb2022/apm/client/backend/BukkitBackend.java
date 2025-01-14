package me.gb2022.apm.client.backend;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class BukkitBackend extends MessageBackend implements Listener {
    private final Plugin provider;

    public BukkitBackend(Plugin provider) {
        this.provider = provider;
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        this.removePlayer(event.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        if (this.handleMessage(message, event.getPlayer().getName())) {
            event.setCancelled(true);
        }
    }

    @Override
    public void start() {
        Bukkit.getPluginManager().registerEvents(this, this.provider);
    }

    @Override
    public void stop() {
        AsyncPlayerChatEvent.getHandlerList().unregister(this);
        PlayerQuitEvent.getHandlerList().unregister(this);
    }

    @Override
    public void sendMessage(String name, String rawMessage) {
        Objects.requireNonNull(Bukkit.getPlayer(name)).sendMessage(rawMessage);
    }
}
