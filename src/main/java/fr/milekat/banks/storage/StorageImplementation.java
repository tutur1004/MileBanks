package fr.milekat.banks.storage;

import fr.milekat.utils.storage.exceptions.StorageExecuteException;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unused")
public interface StorageImplementation extends CacheManager {
    /**
     * Check if all storages are loaded
     * @return true if all storages are loaded
     */
    boolean checkStorages();

    /**
     * Disconnect from Storage provider
     */
    void disconnect();

    /*
            ES Queries execution
     */

    int getMoneyFromTags(@NotNull Map<String, Object> tags) throws StorageExecuteException;

    UUID addMoneyToTags(@NotNull Map<String, Object> tags, int amount, String reason) throws StorageExecuteException;

    default UUID removeMoneyToTags(@NotNull Map<String, Object> tags, int amount, String reason)
            throws StorageExecuteException {
        if (amount > 0) amount = -1 * amount;
        return addMoneyToTags(tags, amount, reason);
    }

    UUID resetMoneyToTags(@NotNull Map<String, Object> tags, String reason) throws StorageExecuteException;
}
