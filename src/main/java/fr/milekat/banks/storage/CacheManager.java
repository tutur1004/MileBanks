package fr.milekat.banks.storage;

import fr.milekat.banks.Main;
import fr.milekat.banks.storage.exceptions.StorageExecuteException;
import fr.milekat.banks.utils.BankAccount;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public interface CacheManager {
    default int getCacheBalance(@NotNull Map<String, Object> tags) throws StorageExecuteException {
        Main.debug("Get cache account with tags: " + tags + ".");
        Optional<Map.Entry<BankAccount, Date>> optionalAccount = Storage.BANK_ACCOUNTS_CACHE.entrySet()
                .stream()
                .filter(entry -> entry.getKey().tags().equals(tags))
                .filter(entry -> entry.getValue().getTime() + Storage.BANK_ACCOUNT_DELAY > new Date().getTime())
                .findFirst();
        if (optionalAccount.isPresent()) {
            Main.debug("Account with tags: " + tags + " found.");
            return optionalAccount.get().getKey().balance();
        } else  {
            Main.debug("Account with tags: " + tags + " not found in cache, try to search it.");
            return Main.getStorage().getTagsMoney(tags);
        }
    }

    static void addCacheAccount(@NotNull Map<BankAccount, Date> cache, @NotNull BankAccount account) {
        List<BankAccount> accounts = new ArrayList<>(cache.keySet());
        if (accounts.stream().anyMatch(loop -> loop.tags().equals(account.tags()))) {
            Map<BankAccount, Date> tempCache = new HashMap<>(cache);
            cache.keySet().stream().filter(loop -> loop.tags().equals(account.tags()))
                    .forEach(tempCache::remove);
            tempCache.put(account, new Date());
            cache = new HashMap<>(tempCache);
        } else {
            cache.put(account, new Date());
        }
        Storage.BANK_ACCOUNTS_CACHE = cache;
    }
}
