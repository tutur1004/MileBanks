package fr.milekat.banks.storage;

import fr.milekat.banks.storage.exceptions.StorageExecuteException;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unused")
public interface StorageImplementation extends CacheManager {
    /**
     * Check if all storages are loaded
     * @return true if all storages are loaded
     */
    boolean checkStorages() throws StorageExecuteException;

    /**
     * Get the implemented (Used) storage type
     * @return storage type
     */
    String getImplementationName();

    /**
     * Disconnect from Storage provider
     */
    void disconnect();

    /*
            ES Queries execution
     */

    int getMoneyFromTag(@NotNull String tagName, @NotNull Object tagValue) throws StorageExecuteException;

    UUID addMoneyToTags(@NotNull Map<String, Object> tags, int amount, String reason) throws StorageExecuteException;

    default UUID removeMoneyToTags(@NotNull Map<String, Object> tags, int amount, String reason)
            throws StorageExecuteException {
        return addMoneyToTags(tags, (-1 * amount), reason);
    }

    UUID setMoneyToTag(@NotNull String tagName, @NotNull Object tagValue, int amount, String reason)
            throws StorageExecuteException;
}
