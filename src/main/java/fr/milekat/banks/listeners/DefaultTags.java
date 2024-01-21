package fr.milekat.banks.listeners;

import fr.milekat.banks.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class DefaultTags implements Listener {

    @EventHandler
    public void setPlayerTags(@NotNull PlayerJoinEvent event) {
        Main.PLAYER_TAGS.put(event.getPlayer().getUniqueId(), Map.of(
                "player-name", event.getPlayer().getName(),
                "player-uuid", event.getPlayer().getUniqueId().toString())
        );
    }

    @EventHandler
    public void removePlayerTags(@NotNull PlayerQuitEvent event) {
        Main.PLAYER_TAGS.remove(event.getPlayer().getUniqueId());
    }
}
