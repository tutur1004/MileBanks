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

    /*
        Get money
     */

    /**
     * Retrieves the amount of money associated with a {@link UUID}.
     *
     * @param player The {@link UUID} of the player.
     * @return A map of tags and their associated amount of money.
     * @throws StorageException if there is an error accessing the storage.
     */
    Map<String, Integer> getMoney(@NotNull UUID player) throws StorageException;

    /**
     * Retrieves the amount of money associated with a specific tag.
     *
     * @param tagName   Tag name.
     * @param tagValue Tag value.
     * @return The amount of money associated with the tag.
     * @throws StorageException if there is an error accessing the storage.
     */
    int getMoneyByTag(@NotNull String tagName, @NotNull Object tagValue) throws StorageException;

    /*
        Add money
     */
    
    /**
     * Adds an amount of money to multiple tags.
     *
     * @param tags   A map of tags, where each tagName represents the tag name and the value of the tag.
     * @param amount The amount of money to add.
     * @param reason Operation reason (Or an operation description).
     * @return Transaction id.
     * @throws StorageException if there is an error while updating the storage.
     */
    UUID addMoneyByTags(@NotNull Map<String, Object> tags,
                        int amount, @Nullable String reason) throws StorageException;
    /**
     * Adds an amount of money to multiple tags.
     *
     * @param player The {@link UUID} of the player.
     * @param tags   A map of tags, where each tagName represents the tag name and the value of the tag.
     * @param amount The amount of money to add.
     * @param reason Operation reason (Or an operation description).
     * @return Transaction id.
     * @throws StorageException if there is an error while updating the storage.
     * @deprecated Use {@link #addMoneyByTag(String, Object, int, String)} or
     * {@link #addMoneyByTags(Map, int, String)} instead.
     */
    @Deprecated
    default UUID addMoneyByTags(@NotNull UUID player, @NotNull Map<String, Object> tags,
                        int amount, @Nullable String reason) throws StorageException {
        return addMoneyByTags(tags, amount, reason);
    }
    /**
     * Adds an amount of money to a specific tag.
     *
     * @param tagName    The name of the tag.
     * @param tagValue  The value of the tag.
     * @param amount The amount of money to add.
     * @param reason Operation reason (Or an operation description).
     * @return Transaction id.
     * @throws StorageException if there is an error while updating the storage.
     */
    default UUID addMoneyByTag(@NotNull String tagName, @NotNull Object tagValue,
                       int amount, @Nullable String reason) throws StorageException {
        return addMoneyByTags(Map.of(tagName, tagValue), amount, reason);
    }
    /**
     * Adds an amount of money to a specific tag.
     *
     * @param player The {@link UUID} of the player.
     * @param tagName    The name of the tag.
     * @param tagValue  The value of the tag.
     * @param amount The amount of money to add.
     * @param reason Operation reason (Or an operation description).
     * @return Transaction id.
     * @throws StorageException if there is an error while updating the storage.
     * @deprecated Use {@link #addMoneyByTag(String, Object, int, String)} or
     * {@link #addMoneyByTags(Map, int, String)} instead.
     */
    @Deprecated
    default UUID addMoneyByTag(@NotNull UUID player, @NotNull String tagName, @NotNull Object tagValue,
                       int amount, @Nullable String reason) throws StorageException {
        return addMoneyByTag(tagName, tagValue, amount, reason);
    }
    
    /*
        Remove money
     */

    /**
     * Removes an amount of money from multiple tags.
     *
     * @param tags   A map of tags, where each tagName represents the tag name and the tagValue of the tag.
     * @param amount The amount of money to remove.
     * @param reason Operation reason (Or an operation description).
     * @return Transaction id.
     * @throws StorageException if there is an error while updating the storage.
     */
    UUID removeMoneyByTags(@NotNull Map<String, Object> tags,
                           int amount, @Nullable String reason) throws StorageException;
    /**
     * Removes an amount of money from multiple tags.
     *
     * @param player The {@link UUID} of the player.
     * @param tags   A map of tags, where each tagName represents the tag name and the value of the tag.
     * @param amount The amount of money to remove.
     * @param reason Operation reason (Or an operation description).
     * @return Transaction id.
     * @throws StorageException if there is an error while updating the storage.
     * @deprecated Use {@link #removeMoneyByTag(String, Object, int, String)} or
     * {@link #removeMoneyByTags(Map, int, String)} instead.
     */
    @Deprecated
    default UUID removeMoneyByTags(@NotNull UUID player, @NotNull Map<String, Object> tags,
                           int amount, @Nullable String reason) throws StorageException {
        return removeMoneyByTags(tags, amount, reason);
    }
    /**
     * Removes an amount of money from a specific tag.
     *
     * @param tagName    The name of the tag.
     * @param tagValue  The value of the tag.
     * @param amount The amount of money to remove.
     * @param reason Operation reason (Or an operation description).
     * @return Transaction id.
     * @throws StorageException if there is an error while updating the storage.
     */
    default UUID removeMoneyByTag(@NotNull String tagName, @NotNull Object tagValue,
                          int amount, @Nullable String reason) throws StorageException {
        return removeMoneyByTags(Map.of(tagName, tagValue), amount, reason);
    }
    /**
     * Removes an amount of money from a specific tag.
     *
     * @param player The {@link UUID} of the player.
     * @param tagName    The name of the tag.
     * @param tagValue  The value of the tag.
     * @param amount The amount of money to remove.
     * @param reason Operation reason (Or an operation description).
     * @return Transaction id.
     * @throws StorageException if there is an error while updating the storage.
     * @deprecated Use {@link #removeMoneyByTag(String, Object, int, String)} or
     * {@link #removeMoneyByTags(Map, int, String)} instead.
     */
    @Deprecated
    default UUID removeMoneyByTag(@NotNull UUID player, @NotNull String tagName, @NotNull Object tagValue,
                          int amount, @Nullable String reason) throws StorageException {
        return removeMoneyByTag(tagName, tagValue, amount, reason);
    }

    /*
        Set money
     */
    
    /**
     * Sets the amount of money for a specific tag.
     *
     * @param tagName    The name of the tag.
     * @param tagValue  The value of the tag.
     * @param amount The new amount of money.
     * @param reason Operation reason (Or an operation description).
     * @return Transaction id.
     * @throws StorageException if there is an error while updating the storage.
     */
    UUID setMoneyByTag(@NotNull String tagName, @NotNull Object tagValue,
                       int amount, @Nullable String reason) throws StorageException;
    /**
     * Sets the amount of money for a specific tag.
     *
     * @param player The {@link UUID} of the player.
     * @param tagName    The name of the tag.
     * @param tagValue  The value of the tag.
     * @param amount The new amount of money.
     * @param reason Operation reason (Or an operation description).
     * @return Transaction id.
     * @throws StorageException if there is an error while updating the storage.
     * @deprecated Use {@link #setMoneyByTag(String, Object, int, String)} or
     * {@link #setMoneyByTag(String, Object, int, String)} instead.
     */
    @Deprecated
    default UUID setMoneyByTag(@NotNull UUID player, @NotNull String tagName, @NotNull Object tagValue,
                               int amount, @Nullable String reason) throws StorageException {
        return setMoneyByTag(tagName, tagValue, amount, reason);
    }

    /*
        Tags (Player tags)
     */

    /**
     * Retrieves the tags associated with a player identified by UUID.
     *
     * @param uuid The {@link UUID} of the player.
     * @return The player's tags as a map, or null if no tags are found.
     */
    @Nullable
    Map<String, Object> getPlayerTags(@NotNull UUID uuid);

    /**
     * Sets the tags associated with a player identified by UUID.
     *
     * @param uuid The {@link UUID} of the player.
     * @param tags The tags to set for the player.
     * @throws IllegalArgumentException if the tags maps not contains all required tags.
     */
    void setPlayerTags(@NotNull UUID uuid, @NotNull Map<String, Object> tags) throws IllegalArgumentException;
}
