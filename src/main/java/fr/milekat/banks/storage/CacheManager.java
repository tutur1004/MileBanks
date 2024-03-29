package fr.milekat.banks.storage;

import fr.milekat.banks.Main;
import fr.milekat.banks.utils.BankAccount;
import fr.milekat.utils.storage.exceptions.StorageExecuteException;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public interface CacheManager {

    default int getCacheBalance(@NotNull String tagName, @NotNull Object tagValue) throws StorageExecuteException {
        if (Main.BANK_ACCOUNT_DELAY == 0) return Main.getStorage().getMoneyFromTag(tagName, tagValue);
        Main.getMileLogger().debug("Get cache account for tag: " + tagName + ".");
        Optional<Map.Entry<BankAccount, Date>> optionalAccount = Main.BANK_ACCOUNTS_CACHE.entrySet()
                .stream()
                .filter(entry -> entry.getKey().tagName().equals(tagName))
                .filter(entry -> entry.getKey().tagValue().equals(tagValue))
                .filter(entry -> entry.getValue().getTime() + Main.BANK_ACCOUNT_DELAY > new Date().getTime())
                .findFirst();
        if (optionalAccount.isPresent()) {
            Main.getMileLogger().debug("Account with tags: " + tagName + " found.");
            return optionalAccount.get().getKey().balance();
        } else  {
            Main.getMileLogger().debug("Account with tags: " + tagName + " not found in cache, try to search it.");
            return Main.getStorage().getMoneyFromTag(tagName, tagValue);
        }
    }

    static void addCacheAccount(@NotNull Map<BankAccount, Date> cache, @NotNull BankAccount account) {
        if (Main.BANK_ACCOUNT_DELAY == 0) return;
        if (cache.size() >= Main.BANK_ACCOUNTS_CACHE_SIZE) cleanCache(cache);
        List<BankAccount> accounts = new ArrayList<>(cache.keySet());
        if (accounts.stream().anyMatch(loop -> loop.tagName().equals(account.tagName()) &&
                loop.tagValue().equals(account.tagValue()))) {
            Map<BankAccount, Date> tempCache = new HashMap<>(cache);
            cache.keySet().stream()
                    .filter(loop -> loop.tagName().equals(account.tagName()))
                    .filter(loop -> loop.tagValue().equals(account.tagValue()))
                    .forEach(tempCache::remove);
            tempCache.put(account, new Date());
            cache = new HashMap<>(tempCache);
        } else {
            cache.put(account, new Date());
        }
        Main.BANK_ACCOUNTS_CACHE = cache;
    }

    private static void cleanCache(@NotNull Map<BankAccount, Date> cache) {
        if (Main.BANK_ACCOUNTS_CACHE_SIZE == 0 || Main.BANK_ACCOUNT_DELAY == 0) return;
        new HashMap<>(cache).entrySet().stream()
                .filter(date -> date.getValue().getTime() + Main.BANK_ACCOUNT_DELAY < new Date().getTime())
                .map(Map.Entry::getKey)
                .forEach(cache::remove);
    }
}
