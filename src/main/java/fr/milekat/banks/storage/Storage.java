package fr.milekat.banks.storage;

import fr.milekat.banks.Main;
import fr.milekat.banks.storage.adapter.elasticsearch.ESStorage;
import fr.milekat.banks.storage.exceptions.StorageExecuteException;
import fr.milekat.banks.storage.exceptions.StorageLoaderException;
import fr.milekat.banks.utils.BankAccount;
import fr.milekat.utils.Configs;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Storage {
    public static final long BANK_ACCOUNT_DELAY = TimeUnit.MILLISECONDS.convert(5L, TimeUnit.SECONDS);
    public static Map<BankAccount, Date> BANK_ACCOUNTS_CACHE = new HashMap<>();
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
