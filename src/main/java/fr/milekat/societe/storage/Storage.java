package fr.milekat.societe.storage;

import fr.milekat.societe.Main;
import fr.milekat.societe.storage.adapter.elasticsearch.ESStorage;
import fr.milekat.societe.storage.exceptions.StorageExecuteException;
import fr.milekat.societe.storage.exceptions.StorageLoaderException;
import fr.milekat.utils.Configs;
import org.jetbrains.annotations.NotNull;

public class Storage {
    private final StorageImplementation executor;

    public Storage(@NotNull Configs config) throws StorageLoaderException {
        String storageType = config.getString("storage.type");
        Main.debug("Loading storage type: " + storageType);
        if (storageType.equalsIgnoreCase("elasticsearch")) {
            executor = new ESStorage(config);
        } else {
            throw new StorageLoaderException("Unsupported storage type");
        }
        try {
            if (executor.checkStorages()) {
                Main.debug("Storage loaded");
            } else {
                throw new StorageLoaderException("Storages are not loaded properly");
            }
        } catch (StorageExecuteException exception) {
            throw new StorageLoaderException("Can't load storage properly");
        }
    }

    public StorageImplementation getStorageImplementation() {
        return this.executor;
    }
}
