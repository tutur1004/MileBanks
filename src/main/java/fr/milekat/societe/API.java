package fr.milekat.societe;

import fr.milekat.societe.api.SocieteMileKatIAPI;
import fr.milekat.societe.api.exceptions.StorageException;
import fr.milekat.societe.listeners.DefaultTags;
import fr.milekat.societe.storage.exceptions.StorageExecuteException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class API implements SocieteMileKatIAPI {

    @Override
    public boolean isDebug() {
        return Main.DEBUG;
    }

    @Override
    public int getMoney(@NotNull UUID player) throws StorageException {
        try {
            return Main.getStorage().getMoney(player);
        } catch (StorageExecuteException exception) {
            throw new StorageException(exception, exception.getMessage());
        }
    }

    @Override
    public int getMoneyByTag(@NotNull String key, @NotNull Object value) throws StorageException {
        return getMoneyByTags(Map.of(key, value));
    }
    @Override
    public int getMoneyByTags(@NotNull Map<String, Object> tags) throws StorageException {
        try {
            return Main.getStorage().getTagsMoney(tags);
        } catch (StorageExecuteException exception) {
            throw new StorageException(exception, exception.getMessage());
        }
    }

    @Override
    public UUID addMoneyByTag(@NotNull UUID player, @NotNull String key, @NotNull Object value,
                              int amount, @Nullable String reason) throws StorageException {
        return addMoneyByTags(player, Map.of(key, value), amount, reason);
    }
    @Override
    public UUID addMoneyByTags(@NotNull UUID player, @NotNull Map<String, Object> tags,
                               int amount, @Nullable String reason) throws StorageException {
        try {
            return Main.getStorage().addMoneyToTags(player, tags, amount, Objects.requireNonNullElse(reason,
                    "No reason provided, using API"));
        } catch (StorageExecuteException exception) {
            throw new StorageException(exception, exception.getMessage());
        }
    }

    @Override
    public UUID removeMoneyByTag(@NotNull UUID player, @NotNull String key, @NotNull Object value,
                                 int amount, @Nullable String reason) throws StorageException {
        return removeMoneyByTags(player, Map.of(key, value), amount, reason);
    }
    @Override
    public UUID removeMoneyByTags(@NotNull UUID player, @NotNull Map<String, Object> tags,
                                  int amount, @Nullable String reason) throws StorageException {
        try {
            return Main.getStorage().removeMoneyToTags(player, tags, amount, Objects.requireNonNullElse(reason,
                    "No reason provided, using API"));
        } catch (StorageExecuteException exception) {
            throw new StorageException(exception, exception.getMessage());
        }
    }

    @Override
    public UUID setMoneyByTag(@NotNull UUID player, @NotNull String key, @NotNull Object value,
                              int amount, @Nullable String reason) throws StorageException {
        return setMoneyByTags(player, Map.of(key, value), amount, reason);
    }
    @Override
    public UUID setMoneyByTags(@NotNull UUID player, @NotNull Map<String, Object> tags,
                               int amount, @Nullable String reason) throws StorageException {
        try {
            return Main.getStorage().setMoneyToTags(player, tags, amount, Objects.requireNonNullElse(reason,
                    "No reason provided, using API"));
        } catch (StorageExecuteException exception) {
            throw new StorageException(exception, exception.getMessage());
        }
    }

    @Override
    public @Nullable Map<String, Object> getPlayerTags(@NotNull UUID uuid) {
        return DefaultTags.playerTags.getOrDefault(uuid, null);
    }

    @Override
    public void removePlayerTags(@NotNull UUID uuid) {
        DefaultTags.playerTags.remove(uuid);
    }

    @Override
    public void setPlayerTags(@NotNull UUID uuid, @NotNull Map<String, Object> tags) {
        DefaultTags.playerTags.put(uuid, tags);
    }
}
