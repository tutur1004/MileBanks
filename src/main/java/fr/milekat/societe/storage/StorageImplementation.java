package fr.milekat.societe.storage;

import fr.milekat.societe.storage.exceptions.StorageExecuteException;
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

    int getMoney(@NotNull UUID player) throws StorageExecuteException;

    int getTagsMoney(@NotNull Map<String, Object> tags) throws StorageExecuteException;


    UUID addMoneyToTags(@NotNull UUID player, Map<String, Object> tags, int amount, String reason)
            throws StorageExecuteException;

    default UUID removeMoneyToTags(@NotNull UUID player, @NotNull Map<String, Object> tags, int amount, String reason)
            throws StorageExecuteException {
        return addMoneyToTags(player, tags, (-1 * amount), reason);
    }

    UUID setMoneyToTags(@NotNull UUID player, @NotNull Map<String, Object> tags, int amount, String reason)
            throws StorageExecuteException;
}