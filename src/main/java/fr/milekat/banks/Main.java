package fr.milekat.banks;

import fr.milekat.banks.api.MileBanksAPI;
import fr.milekat.banks.commands.MoneyCmd;
import fr.milekat.banks.listeners.DefaultTags;
import fr.milekat.banks.storage.Storage;
import fr.milekat.banks.storage.StorageImplementation;
import fr.milekat.banks.storage.exceptions.StorageLoaderException;
import fr.milekat.utils.Configs;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main extends JavaPlugin {
    private static JavaPlugin plugin;
    private static Configs config;
    public static Boolean DEBUG = false;
    private static Storage LOADED_STORAGE;
    public static final Map<UUID, Map<String, Object>> playerTags = new HashMap<>();

    @Override
    public void onEnable() {
        plugin = this;
        //  Load configs
        try {
            reloadConfigs();
        } catch (NullPointerException exception) {
            warning("Error: " + exception.getLocalizedMessage());
            Main.stack(exception.getStackTrace());
            warning("Configs load failed, disabling plugin..");
            this.onDisable();
            return;
        }
        //  Load storage
        try {
            reloadStorage();
        } catch (StorageLoaderException exception) {
            warning("Error: " + exception.getLocalizedMessage());
            Main.stack(exception.getStackTrace());
            warning("Storage load failed, disabling plugin..");
            this.onDisable();
            return;
        }
        //  Load API
        MileBanksAPI.LOADED_API = new API();
        MileBanksAPI.API_READY = true;
        //  Load plugin listeners
        if (config.getBoolean("enable_builtin_tags", true)) {
            plugin.getServer().getPluginManager().registerEvents(new DefaultTags(), this);
        }
        //  Load plugin commands
        if (config.getBoolean("enable_builtin_commands", true)) {
            PluginCommand moneyCmd = plugin.getCommand("money");
            if (moneyCmd != null) moneyCmd.setExecutor(new MoneyCmd());
        }
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
     * Reload configs
     */
    public static void reloadConfigs() throws NullPointerException {
        // If config file doesn't exist, create it
        if (!plugin.getDataFolder().exists()) if (plugin.getDataFolder().mkdir()) info("Plugin folder created");
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) plugin.saveDefaultConfig();
        config = new Configs(configFile);
        DEBUG = config.getBoolean("debug", false);
        debug("Debug enable");
        info("Config loaded");
    }

    /**
     * Reload storage
     */
    public static void reloadStorage() throws StorageLoaderException {
        try {
            getStorage().disconnect();
        } catch (Exception ignored) {}
        LOADED_STORAGE = new Storage(config);
        debug("Storage enable, API is now available");
    }

    /**
     * Get the plugin instance
     * @return bukkit plugin instance
     */
    public static JavaPlugin getInstance() {
        return plugin;
    }
}
