package fr.milekat.banks;

import fr.milekat.banks.api.MileBanksAPI;
import fr.milekat.banks.commands.MoneyCmd;
import fr.milekat.banks.listeners.DefaultTags;
import fr.milekat.banks.storage.StorageImplementation;
import fr.milekat.banks.storage.adapter.elasticsearch.ESStorage;
import fr.milekat.banks.utils.BankAccount;
import fr.milekat.utils.Configs;
import fr.milekat.utils.MileLogger;
import fr.milekat.utils.storage.StorageConnection;
import fr.milekat.utils.storage.StorageLoader;
import fr.milekat.utils.storage.StorageVendor;
import fr.milekat.utils.storage.exceptions.StorageLoadException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Main extends JavaPlugin {
    private static JavaPlugin plugin;
    private static MileLogger logger;
    private static Configs config;
    public static Boolean DEBUG = false;
    public static String PREFIX;
    private static StorageImplementation STORAGE;
    public static long BANK_ACCOUNT_DELAY = TimeUnit.MILLISECONDS.convert(5L, TimeUnit.SECONDS);
    public static Map<BankAccount, Date> BANK_ACCOUNTS_CACHE = new HashMap<>();
    public static int BANK_ACCOUNTS_CACHE_SIZE = 1000;
    public static final Map<String, Class<?>> TAGS = new HashMap<>();
    public static final Map<UUID, Map<String, Object>> PLAYER_TAGS = new HashMap<>();

    @Override
    public void onEnable() {
        plugin = this;
        logger = new MileLogger(this.getLogger());
        //  Load configs
        try {
            reloadConfigs();
        } catch (NullPointerException exception) {
            logger.warning("Error: " + exception.getLocalizedMessage());
            logger.warning("Configs load failed, disabling plugin..");
            this.onDisable();
            return;
        }
        //  Load storage
        try {
            reloadStorage();
        } catch (StorageLoadException exception) {
            logger.warning("Error: " + exception.getLocalizedMessage());
            logger.stack(exception.getStackTrace());
            logger.warning("Storage load failed, disabling plugin..");
            this.onDisable();
            return;
        }
        //  Load API
        MileBanksAPI.LOADED_API = new API();
        MileBanksAPI.API_READY = true;
        //  Load plugin listeners
        if (config.getBoolean("tags.enable_builtin_tags", true)) {
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

    @Contract(" -> new")
    public static @NotNull MileLogger getMileLogger() {
        return logger;
    }

    /**
     * Send a formatted message to sender
     */
    public static void message(@NotNull CommandSender sender, @NotNull String message) {
        if (sender instanceof Player player) {
            message(player, message);
        } else {
            logger.info(message);
        }
    }

    /**
     * Send a formatted message to sender
     */
    public static void message(@NotNull Player player, @NotNull String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                PREFIX + ChatColor.RESET + message));
    }

    /**
     * Get Storage
     * @return Storage implementation
     */
    public static StorageImplementation getStorage() {
        return STORAGE;
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
        if (!plugin.getDataFolder().exists()) if (plugin.getDataFolder().mkdir()) logger.info("Plugin folder created");
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) plugin.saveDefaultConfig();
        config = new Configs(configFile);
        TAGS.clear();
        if (config.getBoolean("tags.enable_builtin_tags", true)) {
            TAGS.put("player-uuid", String.class);
            TAGS.put("player-name", String.class);
        } else {
            config.getStringList("tags.custom.string").forEach(tag -> TAGS.put(tag, String.class));
            config.getStringList("tags.custom.integer").forEach(tag -> TAGS.put(tag, Integer.class));
            config.getStringList("tags.custom.long").forEach(tag -> TAGS.put(tag, Float.class));
            config.getStringList("tags.custom.double").forEach(tag -> TAGS.put(tag, Double.class));
            config.getStringList("tags.custom.boolean").forEach(tag -> TAGS.put(tag, Boolean.class));
        }
        DEBUG = config.getBoolean("debug", false);
        logger.setDebug(DEBUG);
        PREFIX = ChatColor.translateAlternateColorCodes('&',
                config.getString("messages.prefix", "[" + plugin.getName() + "] "));
        logger.debug("Debug enable");
        logger.info("Config loaded");
    }

    /**
     * Reload storage
     */
    public static void reloadStorage() throws StorageLoadException {
        try {
            getStorage().disconnect();
        } catch (Exception ignored) {}
        StorageConnection connection = new StorageLoader(config, logger).getLoadedConnection();
        if (Objects.requireNonNull(connection.getVendor()) == StorageVendor.ELASTICSEARCH) {
            STORAGE = new ESStorage(config);
        } else {
            throw new StorageLoadException("Unsupported storage type");
        }
        if (!STORAGE.checkStorages()) {
            throw new StorageLoadException("Storages are not loaded properly");
        }
        if (config.getBoolean("storage.cache.enable", true)) {
            Main.BANK_ACCOUNT_DELAY = TimeUnit.MILLISECONDS.convert(
                config.getLong("storage.cache.time", 5L), TimeUnit.SECONDS);
            logger.debug("Account cache delay set to " + Main.BANK_ACCOUNT_DELAY + "ms");
            Main.BANK_ACCOUNTS_CACHE_SIZE = config.getInt("storage.cache.size", 1000);
            logger.debug("Account cache size set to " + Main.BANK_ACCOUNTS_CACHE_SIZE);
        } else {
            Main.BANK_ACCOUNT_DELAY = 0L;
            logger.debug("Accounts cache disabled");
        }
        Main.BANK_ACCOUNTS_CACHE.clear();
        logger.debug("Storage enable, API is now available");
    }

    /**
     * Get the plugin instance
     * @return bukkit plugin instance
     */
    public static JavaPlugin getInstance() {
        return plugin;
    }
}
