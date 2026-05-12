package fr.milekat.banks.api;

import fr.milekat.banks.api.classes.BankAccount;
import fr.milekat.banks.api.exceptions.StorageException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
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
        Currencies
     */

    /**
     * Returns true if more than one currency is configured.
     * When true, all money operations require a "currency" tag.
     */
    boolean isMultiCurrency();

    /**
     * Returns the list of all configured currencies.
     * Empty when {@link #isMultiCurrency()} is false.
     *
     * @return immutable list of currency names.
     */
    @NotNull List<String> getCurrencies();

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
    default int getMoneyByTag(@NotNull String tagName, @NotNull Object tagValue) throws StorageException {
        return getMoneyByTags(Map.of(tagName, tagValue));
    }

    /**
     * Retrieves the amount of money associated with a specific tags.
     *
     * @param tags   A map of tags, where each tagName represents the tag name and the value of the tag.
     * @return The amount of money associated with the tag.
     * @throws StorageException if there is an error accessing the storage.
     */
    int getMoneyByTags(@NotNull Map<String, Object> tags) throws StorageException;

    /**
     * Retrieves a paginated, rank-ordered list of bank accounts matching the given tags.
     * <p>
     * Results are sorted by balance descending. The map key represents the global rank of each
     * account (e.g. page 0 → ranks 1...size, page 1 → ranks size+1...2*size).
     * Pass an empty {@code tags} map to match all accounts regardless of tags.
     *
     * @param tags A map of tag names to values used to filter accounts.
     *             Pass an empty map to retrieve accounts without tag filtering.
     * @param size The maximum number of accounts to return per page.
     * @param page The zero-based page index.
     * @return A {@link Map} where each key is the global rank (1-based) and the value is the
     *         corresponding {@link BankAccount}, ordered by descending balance.
     * @throws StorageException if there is an error accessing the storage.
     */
    Map<Integer, BankAccount> getBankAccountsFromTags(@NotNull Map<String, Object> tags, int size, int page)
            throws StorageException;

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
     * Resets the balance for a specific set of tags to the given amount (default 0).
     * Use this method when multiple currencies are configured.
     *
     * @param tags   A map of tags that identify the account to reset (must include "currency" when multi-currency).
     * @param amount The amount to set the balance to after reset.
     * @param reason Operation reason (Or an operation description).
     * @return Transaction id.
     * @throws StorageException if there is an error while updating the storage.
     */
    UUID resetMoneyByTags(@NotNull Map<String, Object> tags,
                          int amount, @Nullable String reason) throws StorageException;

    /**
     * Resets the balance for a specific set of tags to 0.
     *
     * @param tags   A map of tags that identify the account to reset (must include "currency" when multi-currency).
     * @param reason Operation reason (Or an operation description).
     * @return Transaction id.
     * @throws StorageException if there is an error while updating the storage.
     */
    default UUID resetMoneyByTags(@NotNull Map<String, Object> tags,
                                  @Nullable String reason) throws StorageException {
        return resetMoneyByTags(tags, 0, reason);
    }

    /**
     * Resets the balance for a specific tag to the given amount (default 0).
     * When multiple currencies are configured, use {@link #resetMoneyByTags(Map, int, String)} instead.
     *
     * @param tagName  The name of the tag.
     * @param tagValue The value of the tag.
     * @param amount   The amount to set the balance to after reset.
     * @param reason   Operation reason (Or an operation description).
     * @return Transaction id.
     * @throws StorageException if there is an error while updating the storage.
     */
    UUID resetMoneyByTag(@NotNull String tagName, @NotNull Object tagValue,
                         int amount, @Nullable String reason) throws StorageException;

    /**
     * Resets the balance for a specific tag to 0.
     * When multiple currencies are configured, use {@link #resetMoneyByTags(Map, int, String)} instead.
     *
     * @param tagName  The name of the tag.
     * @param tagValue The value of the tag.
     * @param reason   Operation reason (Or an operation description).
     * @return Transaction id.
     * @throws StorageException if there is an error while updating the storage.
     */
    default UUID resetMoneyByTag(@NotNull String tagName, @NotNull Object tagValue,
                                 @Nullable String reason) throws StorageException {
        return resetMoneyByTag(tagName, tagValue, 0, reason);
    }
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
     * @deprecated Use {@link #resetMoneyByTag(String, Object, String)} or
     * {@link #resetMoneyByTag(String, Object, String)} instead.
     */
    @Deprecated
    default UUID resetMoneyByTag(@NotNull UUID player, @NotNull String tagName, @NotNull Object tagValue,
                                 int amount, @Nullable String reason) throws StorageException {
        return resetMoneyByTag(tagName, tagValue, amount, reason);
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
     * Removes the tags associated with a player identified by UUID.
     *
     * @param uuid The UUID of the player.
     */
    void removePlayerTags(@NotNull UUID uuid);
    /**
     * Sets the tags associated with a player identified by UUID.
     *
     * @param uuid The {@link UUID} of the player.
     * @param tags The tags to set for the player.
     * @throws IllegalArgumentException if the tags maps not contains all required tags.
     */
    void setPlayerTags(@NotNull UUID uuid, @NotNull Map<String, Object> tags) throws IllegalArgumentException;
}
