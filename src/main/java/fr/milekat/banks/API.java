package fr.milekat.banks;

import fr.milekat.banks.api.MileBanksIAPI;
import fr.milekat.banks.api.exceptions.StorageException;
import fr.milekat.banks.storage.exceptions.StorageExecuteException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class API implements MileBanksIAPI {

    @Override
    public boolean isDebug() {
        return Main.DEBUG;
    }

    @Override
    public int getMoney(@NotNull UUID player) throws StorageException {
        try {
            if (!Main.playerTags.containsKey(player)) {
                return 0;
            }
            return Main.getStorage().getCacheBalance(Main.playerTags.get(player));
        } catch (StorageExecuteException exception) {
            throw new StorageException(exception, exception.getMessage());
        }
    }

    @Override
    public int getMoneyByTags(@NotNull Map<String, Object> tags) throws StorageException {
        try {
            return Main.getStorage().getCacheBalance(tags);
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
    public UUID removeMoneyByTags( @NotNull Map<String, Object> tags, int amount,
                                   @Nullable String reason) throws StorageException {
        try {
            return Main.getStorage().removeMoneyToTags(tags, amount, Objects.requireNonNullElse(reason,
                    "No reason provided, using API"));
        } catch (StorageExecuteException exception) {
            throw new StorageException(exception, exception.getMessage());
        }
    }

    @Override
    public UUID setMoneyByTags(@NotNull Map<String, Object> tags, int amount,
                               @Nullable String reason) throws StorageException {
        try {
            return Main.getStorage().setMoneyToTags(tags, amount, Objects.requireNonNullElse(reason,
                    "No reason provided, using API"));
        } catch (StorageExecuteException exception) {
            throw new StorageException(exception, exception.getMessage());
        }
    }

    @Override
    public @Nullable Map<String, Object> getPlayerTags(@NotNull UUID uuid) {
        return Main.playerTags.getOrDefault(uuid, null);
    }

    @Override
    public void removePlayerTags(@NotNull UUID uuid) {
        Main.playerTags.remove(uuid);
    }

    @Override
    public void setPlayerTags(@NotNull UUID uuid, @NotNull Map<String, Object> tags) {
        Main.playerTags.put(uuid, tags);
    }
}
