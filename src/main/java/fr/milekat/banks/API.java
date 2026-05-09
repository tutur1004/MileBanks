package fr.milekat.banks;

import fr.milekat.banks.api.MileBanksIAPI;
import fr.milekat.banks.api.exceptions.MissingCurrencyException;
import fr.milekat.banks.api.exceptions.StorageException;
import fr.milekat.banks.api.exceptions.UnknownCurrencyException;
import fr.milekat.utils.storage.exceptions.StorageExecuteException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class API implements MileBanksIAPI {

    @Override
    public boolean isDebug() {
        return Main.DEBUG;
    }

    @Override
    public boolean isMultiCurrency() {
        return Main.isMultiCurrency();
    }

    @Override
    public @NotNull List<String> getCurrencies() {
        return Collections.unmodifiableList(Main.CURRENCIES);
    }

    /**
     * Throws if multiple currencies are configured and the tags map does not contain "currency",
     * or if the provided currency value is not in the configured list.
     */
    private void requireCurrencyTag(@NotNull Map<String, Object> tags) throws StorageException {
        if (!Main.isMultiCurrency()) return;
        if (!tags.containsKey("currency")) {
            throw new MissingCurrencyException(Main.CURRENCIES);
        }
        String currency = tags.get("currency").toString();
        if (!Main.CURRENCIES.contains(currency)) {
            throw new UnknownCurrencyException(currency, Main.CURRENCIES);
        }
    }

    @Override
    public Map<String, Integer> getMoney(@NotNull UUID player) throws StorageException {
        try {
            if (!Main.PLAYER_TAGS.containsKey(player)) {
                return new HashMap<>();
            }
            Map<String, Integer> tagsBalances = new HashMap<>();
            for (Map.Entry<String, Object> entry : Main.PLAYER_TAGS.get(player).entrySet()) {
                tagsBalances.put(entry.getKey(), Main.getStorage()
                        .getCacheBalance(Map.of(entry.getKey(), entry.getValue())));
            }
            return tagsBalances;
        } catch (StorageExecuteException exception) {
            throw new StorageException(exception, exception.getMessage());
        }
    }

    @Override
    public int getMoneyByTags(@NotNull Map<String, Object> tags) throws StorageException {
        requireCurrencyTag(tags);
        try {
            return Main.getStorage().getCacheBalance(tags);
        } catch (StorageExecuteException exception) {
            throw new StorageException(exception, exception.getMessage());
        }
    }

    @Override
    public UUID addMoneyByTags(@NotNull Map<String, Object> tags,
                               int amount, @Nullable String reason) throws StorageException {
        requireCurrencyTag(tags);
        try {
            return Main.getStorage().addMoneyToTags(tags, amount, Objects.requireNonNullElse(reason,
                    "No reason provided, using API"));
        } catch (StorageExecuteException exception) {
            throw new StorageException(exception, exception.getMessage());
        }
    }

    @Override
    public UUID removeMoneyByTags(@NotNull Map<String, Object> tags, int amount,
                                   @Nullable String reason) throws StorageException {
        requireCurrencyTag(tags);
        try {
            return Main.getStorage().removeMoneyToTags(tags, amount, Objects.requireNonNullElse(reason,
                    "No reason provided, using API"));
        } catch (StorageExecuteException exception) {
            throw new StorageException(exception, exception.getMessage());
        }
    }

    @Override
    public UUID resetMoneyByTags(@NotNull Map<String, Object> tags,
                                 @Nullable String reason) throws StorageException {
        requireCurrencyTag(tags);
        try {
            return Main.getStorage().resetMoneyToTags(tags, Objects.requireNonNullElse(reason,
                    "No reason provided, using API"));
        } catch (StorageExecuteException exception) {
            throw new StorageException(exception, exception.getMessage());
        }
    }

    @Override
    public UUID resetMoneyByTag(@NotNull String tagName, @NotNull Object tagValue,
                                @Nullable String reason) throws StorageException {
        if (Main.isMultiCurrency()) {
            throw new MissingCurrencyException(Main.CURRENCIES);
        }
        try {
            return Main.getStorage().resetMoneyToTags(Map.of(tagName, tagValue), Objects.requireNonNullElse(reason,
                    "No reason provided, using API"));
        } catch (StorageExecuteException exception) {
            throw new StorageException(exception, exception.getMessage());
        }
    }

    @Override
    public @Nullable Map<String, Object> getPlayerTags(@NotNull UUID uuid) {
        return Main.PLAYER_TAGS.getOrDefault(uuid, null);
    }

    @Override
    public void removePlayerTags(@NotNull UUID uuid) {
        Main.PLAYER_TAGS.remove(uuid);
    }

    @Override
    public void setPlayerTags(@NotNull UUID uuid, @NotNull Map<String, Object> tags) throws IllegalArgumentException {
        Main.PLAYER_TAGS.put(uuid, tags);
    }
}
