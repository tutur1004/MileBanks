package fr.milekat.banks;

import fr.milekat.banks.api.MileBanksIAPI;
import fr.milekat.banks.api.exceptions.StorageException;
import fr.milekat.banks.storage.exceptions.StorageExecuteException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class API implements MileBanksIAPI {

    @Override
    public boolean isDebug() {
        return Main.DEBUG;
    }

    @Override
    public Map<String, Integer> getMoney(@NotNull UUID player) throws StorageException {
        try {
            if (!Main.PLAYER_TAGS.containsKey(player)) {
                return new HashMap<>();
            }
            Map<String, Integer> tagsBalances = new HashMap<>();
            for (Map.Entry<String, Object> entry : Main.PLAYER_TAGS.get(player).entrySet()) {
                tagsBalances.put(entry.getKey(), Main.getStorage().getCacheBalance(entry.getKey(), entry.getValue()));
            }
            return tagsBalances;
        } catch (StorageExecuteException exception) {
            throw new StorageException(exception, exception.getMessage());
        }
    }

    @Override
    public int getMoneyByTag(@NotNull String tagName, @NotNull Object tagValue) throws StorageException {
        try {
            return Main.getStorage().getCacheBalance(tagName, tagValue);
        } catch (StorageExecuteException exception) {
            throw new StorageException(exception, exception.getMessage());
        }
    }

    @Override
    public UUID addMoneyByTags(@NotNull Map<String, Object> tags,
                               int amount, @Nullable String reason) throws StorageException {
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
        try {
            return Main.getStorage().removeMoneyToTags(tags, amount, Objects.requireNonNullElse(reason,
                    "No reason provided, using API"));
        } catch (StorageExecuteException exception) {
            throw new StorageException(exception, exception.getMessage());
        }
    }

    @Override
    public UUID setMoneyByTag(@NotNull String tagName, @NotNull Object tagValue, int amount,
                               @Nullable String reason) throws StorageException {
        try {
            return Main.getStorage().setMoneyToTag(tagName, tagValue, amount, Objects.requireNonNullElse(reason,
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
    public void setPlayerTags(@NotNull UUID uuid, @NotNull Map<String, Object> tags) throws IllegalArgumentException {
        if (Main.PLAYER_TAGS.containsKey(uuid)) {
            throw new IllegalArgumentException("Missing required tags ! Required: " + Main.PLAYER_TAGS.get(uuid));
        }
        Main.PLAYER_TAGS.put(uuid, tags);
    }
}
