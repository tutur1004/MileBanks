package fr.milekat.banks;

import fr.milekat.banks.api.MilekatBanksAPI;
import fr.milekat.banks.commands.MoneyCmd;
import fr.milekat.banks.listeners.DefaultTags;
import fr.milekat.banks.storage.Storage;
import fr.milekat.banks.storage.StorageImplementation;
import fr.milekat.banks.storage.exceptions.StorageLoaderException;
import fr.milekat.utils.Configs;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

public class Main extends JavaPlugin {
    private static JavaPlugin plugin;
    private static Configs config;
    public static Boolean DEBUG = false;
    private static Storage LOADED_STORAGE;

        @Override
    public void onEnable() {
        plugin = this;
        //  Load configs
        File configFile;
        try {
            configFile = File.createTempFile(UUID.randomUUID().toString(), UUID.randomUUID().toString());
            this.getConfig().save(configFile);
        } catch (IOException exception) {
            this.getLogger().warning("Error while trying to load config");
            exception.printStackTrace();
            this.onDisable();
            return;
        }
        config = new Configs(configFile);
        DEBUG = config.getBoolean("debug");
        debug("Debug enable");
        //  Load storage
        try {
            LOADED_STORAGE = new Storage(config);
            debug("Storage enable, API is now available");
        } catch (StorageLoaderException exception) {
            warning("Storage load failed, disabling plugin..");
            warning("Error: " + exception.getLocalizedMessage());
            if (DEBUG) exception.printStackTrace();
            this.onDisable();
        }
        //  Load API
        MilekatBanksAPI.LOADED_API = new API();
        MilekatBanksAPI.API_READY = true;
        //  Load plugin workers
        plugin.getServer().getPluginManager().registerEvents(new DefaultTags(), this);
        plugin.getCommand("money").setExecutor(new MoneyCmd());
    }

    @Override
    public void onDisable() {
        try {
            getStorage().disconnect();
        } catch (Exception ignored) {}
    }

    /**
     * Log a debug if debug is enable
     * @param message to debug
     */
    public static void debug(String message) {
        if (DEBUG) plugin.getLogger().info("[DEBUG] " + message);
    }

    /**
     * If debug are enabled, stack traces will be logged at warning level
     * @param stacks to log
     */
    public static void stack(StackTraceElement[] stacks) {
        if (DEBUG) Arrays.stream(stacks).distinct().forEach(stackTraceElement -> warning(stackTraceElement.toString()));
    }

    /**
     * Log a message
     * @param message to send
     */
    public static void info(String message) {
        plugin.getLogger().info(message);
    }

    /**
     * Log a warning
     * @param message to raise
     */
    public static void warning(String message) {
        plugin.getLogger().warning(message);
    }

    /**
     * Send a formatted message to sender
     */
    public static void message(@NotNull CommandSender sender, @NotNull String message) {
        if (sender instanceof Player player) {
            message(player, message);
        } else {
            info(message);
        }
    }

    /**
     * Send a formatted message to sender
     */
    public static void message(@NotNull Player player, @NotNull String message) {
        player.sendMessage(Main.getConfigs().getMessage("messages.prefix") + ChatColor.RESET +
                ChatColor.translateAlternateColorCodes('&', message));
    }

    /**
     * Send a formatted BaseComponent message to sender
     */
    public static void message(@NotNull Player player, @NotNull BaseComponent message) {
        BaseComponent prefixedMessage = new TextComponent(Main.getConfigs().getMessage("messages.prefix") +
                ChatColor.RESET);
        prefixedMessage.addExtra(message.duplicate());
        player.spigot().sendMessage(prefixedMessage);
    }

    /**
     * Get Storage
     * @return Storage implementation
     */
    public static StorageImplementation getStorage() {
        return LOADED_STORAGE.getStorageImplementation();
    }

    /**
     * Get config file
     * @return Config file
     */
    public static Configs getConfigs() {
        return config;
    }

    /**
     * Get the plugin instance
     * @return bukkit plugin instance
     */
    public static JavaPlugin getInstance() {
        return plugin;
    }
}
