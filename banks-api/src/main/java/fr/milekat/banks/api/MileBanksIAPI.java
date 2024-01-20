package fr.milekat.banks.api;

import fr.milekat.banks.api.exceptions.StorageException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * The MileBanksIAPI interface provides access to the Banks API functionalities.
 */
@SuppressWarnings("unused")
public interface MileBanksIAPI {
    /**
     * Checks if the API is running in debug mode.
     *
     * @return true if the API is in debug mode, false otherwise.
     */
    boolean isDebug();

    /**
     * Retrieves the amount of money associated with a {@link UUID}.
     *
     * @param player The {@link UUID} of the player.
     * @return The amount of money associated with the given player {@link UUID}.
     * @throws StorageException if there is an error accessing the storage.
     * @deprecated Use {@link #getMoneyByTag(String, Object)} or {@link #getMoneyByTags(Map)} instead.
     */
    @Deprecated
    int getMoney(@NotNull UUID player) throws StorageException;

    /**
     * Retrieves the amount of money associated with a specific tag.
     *
     * @param key   The key of the tag.
     * @param value The value of the tag.
     * @return The amount of money associated with the tag.
     * @throws StorageException if there is an error accessing the storage.
     */
    int getMoneyByTag(@NotNull String key, @NotNull Object value) throws StorageException;

    /**
     * Retrieves the amount of money associated with multiple tags.
     *
     * @param tags A map of tags, where each key represents the key of the tag and the value represents the value of the tag.
     * @return The total amount of money associated with the tags.
     * @throws StorageException if there is an error accessing the storage.
     */
    int getMoneyByTags(@NotNull Map<String, Object> tags) throws StorageException;

    /**
     * Adds an amount of money to a specific tag.
     *
     * @param player The {@link UUID} of the player.
     * @param key    The key of the tag.
     * @param value  The value of the tag.
     * @param amount The amount of money to add.
     * @param reason Operation reason (Or an operation description).
     * @return Transaction id.
     * @throws StorageException if there is an error while updating the storage.
     */
    UUID addMoneyByTag(@NotNull UUID player, @NotNull String key, @NotNull Object value,
                       int amount, @Nullable String reason) throws StorageException;
    /**
     * Adds an amount of money to multiple tags.
     *
     * @param player The {@link UUID} of the player.
     * @param tags   A map of tags, where each key represents the key of the tag and the value represents the value of the tag.
     * @param amount The amount of money to add.
     * @param reason Operation reason (Or an operation description).
     * @return Transaction id.
     * @throws StorageException if there is an error while updating the storage.
     */
    UUID addMoneyByTags(@NotNull UUID player, @NotNull Map<String, Object> tags,
                        int amount, @Nullable String reason) throws StorageException;

    /**
     * Removes an amount of money from a specific tag.
     *
     * @param player The {@link UUID} of the player.
     * @param key    The key of the tag.
     * @param value  The value of the tag.
     * @param amount The amount of money to remove.
     * @param reason Operation reason (Or an operation description).
     * @return Transaction id.
     * @throws StorageException if there is an error while updating the storage.
     */
    UUID removeMoneyByTag(@NotNull UUID player, @NotNull String key, @NotNull Object value,
                          int amount, @Nullable String reason) throws StorageException;
    /**
     * Removes an amount of money from multiple tags.
     *
     * @param player The {@link UUID} of the player.
     * @param tags   A map of tags, where each key represents the key of the tag and the value represents the value of the tag.
     * @param amount The amount of money to remove.
     * @param reason Operation reason (Or an operation description).
     * @return Transaction id.
     * @throws StorageException if there is an error while updating the storage.
     */
    UUID removeMoneyByTags(@NotNull UUID player, @NotNull Map<String, Object> tags,
                           int amount, @Nullable String reason) throws StorageException;

    /**
     * Sets the amount of money for a specific tag.
     *
     * @param player The {@link UUID} of the player.
     * @param key    The key of the tag.
     * @param value  The value of the tag.
     * @param amount The new amount of money.
     * @param reason Operation reason (Or an operation description).
     * @return Transaction id.
     * @throws StorageException if there is an error while updating the storage.
     */
    UUID setMoneyByTag(@NotNull UUID player, @NotNull String key, @NotNull Object value,
                       int amount, @Nullable String reason) throws StorageException;
    /**
     * Sets the amount of money for multiple tags.
     *
     * @param player The {@link UUID} of the player.
     * @param tags   A map of tags, where each key represents the key of the tag and the value represents the value of the tag.
     * @param amount The new amount of money.
     * @param reason Operation reason (Or an operation description).
     * @return Transaction id.
     * @throws StorageException if there is an error while updating the storage.
     */
    UUID setMoneyByTags(@NotNull UUID player, @NotNull Map<String, Object> tags,
                        int amount, @Nullable String reason) throws StorageException;

    /**
     * Retrieves the tags associated with a player identified by UUID.
     *
     * @param uuid The {@link UUID} of the player.
     * @return The player's tags as a map, or null if no tags are found.
     */
    @Nullable
    Map<String, Object> getPlayerTags(@NotNull UUID uuid);
    /**
     * Removes the tags associated with a player identified by UUID.
     *
     * @param uuid The {@link UUID} of the player.
     */
    void removePlayerTags(@NotNull UUID uuid);
    /**
     * Sets the tags associated with a player identified by UUID.
     *
     * @param uuid The {@link UUID} of the player.
     * @param tags The tags to set for the player.
     */
    void setPlayerTags(@NotNull UUID uuid, @NotNull Map<String, Object> tags);
}
