package fr.milekat.banks.listeners;

import fr.milekat.banks.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DefaultTags implements Listener {
    public static final Map<UUID, Map<String, Object>> playerTags = new HashMap<>();

    @EventHandler
    public void setPlayerTags(@NotNull PlayerJoinEvent event) {
        if (Main.getConfigs().getBoolean("default_tags", true)) {
            playerTags.put(event.getPlayer().getUniqueId(), Map.of("name", event.getPlayer().getName(),
                    "uuid", event.getPlayer().getUniqueId().toString()));
        }
    }

    @EventHandler
    public void removePlayerTags(@NotNull PlayerQuitEvent event) {
        if (Main.getConfigs().getBoolean("default_tags", true)) {
            playerTags.remove(event.getPlayer().getUniqueId());
        }
    }
}
