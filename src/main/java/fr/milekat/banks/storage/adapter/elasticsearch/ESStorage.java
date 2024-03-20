package fr.milekat.banks.storage.adapter.elasticsearch;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.CreateOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.milekat.banks.Main;
import fr.milekat.banks.api.events.MoneyPrepareOperation;
import fr.milekat.banks.api.events.MoneySavedSuccessfully;
import fr.milekat.banks.storage.CacheManager;
import fr.milekat.banks.storage.Storage;
import fr.milekat.banks.storage.StorageImplementation;
import fr.milekat.banks.storage.adapter.elasticsearch.loaders.Index;
import fr.milekat.banks.storage.adapter.elasticsearch.loaders.Transforms;
import fr.milekat.banks.storage.exceptions.StorageExecuteException;
import fr.milekat.banks.storage.exceptions.StorageLoaderException;
import fr.milekat.banks.utils.BankAccount;
import fr.milekat.utils.Configs;
import fr.milekat.utils.DateMileKat;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

public class ESStorage implements StorageImplementation {
    private final ESConnector DB;
    private final String BANK_INDEX_TRANSACTIONS;
    private final Map<String, Class<?>> transactions_fields = new HashMap<>();

    private final String BANK_INDEX_ACCOUNTS;
    private final Map<String, Class<?>> accounts_fields = new HashMap<>();
    private final String numberOfReplicas;
    private final Map<UUID, BulkOperation> moneyOperations = new HashMap<>();

    /*
        Main DB
    */
    public ESStorage(@NotNull Configs config) throws StorageLoaderException {
        String prefix = config.getString("storage.elasticsearch.prefix", "bank-");
        if (!prefix.matches("[a-z0-9][a-z0-9-]{0,19}")) {
            throw new StorageLoaderException("Elasticsearch prefix wrong, please only lower cases (a-z), " +
                    "digits (0-9) and dashed '-', also you can't start with a '-'.");
        }
        this.BANK_INDEX_TRANSACTIONS = prefix + "transactions";
        this.BANK_INDEX_ACCOUNTS = prefix + "accounts";
        this.numberOfReplicas = config.getString("storage.elasticsearch.replicas", "0");
        transactions_fields.put("uuid", UUID.class);
        transactions_fields.put("operation", Double.class);
        transactions_fields.put("reason", String.class);
        transactions_fields.put("transactionId", UUID.class);
        transactions_fields.put("@timestamp", Date.class);
        accounts_fields.put("amount", Integer.class);
        accounts_fields.putAll(Main.TAGS);
        DB = new ESConnector(config);
        try (ESConnection connection = DB.getConnection()) {
            Main.debug(connection.getClient().cluster().health().toString());
            saveOperation();
        } catch (IOException exception) {
            throw new StorageLoaderException("Error while trying to load ElasticSearch cluster");
        }
    }

    @Override
    public boolean checkStorages() {
        Main.debug("Check if storage is ready...");
        String TAGS_FIELD = "tags";
        try (ESConnection connection = DB.getConnection()) {
            Main.debug("Check indices...");
            new Index(connection.getClient(), BANK_INDEX_TRANSACTIONS, numberOfReplicas,
                    transactions_fields, Main.TAGS, TAGS_FIELD);
            new Index(connection.getClient(), BANK_INDEX_ACCOUNTS, numberOfReplicas,
                    accounts_fields, new HashMap<>(), "");
            Main.debug("Check transforms...");
            for (Map.Entry<String, Class<?>> tag : Main.TAGS.entrySet()) {
                new Transforms(connection.getClient(), BANK_INDEX_TRANSACTIONS, BANK_INDEX_ACCOUNTS,
                        Map.of(tag.getKey(), tag.getValue()));
            }
            Main.debug("Storage is ready.");
            return true;
        } catch (IOException | StorageLoaderException exception) {
            Main.warning("ElasticSearch load storage error.");
            Main.stack(exception.getStackTrace());
        }
        return false;
    }

    @Override
    public String getImplementationName() {
        return null;
    }

    @Override
    public void disconnect() {
        Main.debug("ElasticSearch automatically close connections after execution, using try-with-resources.");
    }

    /*
        ES Queries execution
     */

    @Override
    public int getMoneyFromTag(@NotNull String tagName, @NotNull Object tagValue) throws StorageExecuteException {
        Main.debug("[ES-Sync] getMoneyFromTag - search money with tag '" + tagName + "=" + tagValue + "'.");
        BoolQuery.Builder boolQuery = ESUtils.getBuilder(tagName, tagValue);
        SearchRequest request = new SearchRequest.Builder()
                        .index(BANK_INDEX_ACCOUNTS)
                        .query(q -> q.bool(boolQuery.build()))
                        .size(1)
                        .build();
        int balance = fetchMoney(request);
        CacheManager.addCacheAccount(Storage.BANK_ACCOUNTS_CACHE, new BankAccount(tagName, tagValue, balance));
        return balance;
    }

    private int fetchMoney(@NotNull SearchRequest request) throws StorageExecuteException {
        try (ESConnection connection = DB.getConnection()) {
            try {
                SearchResponse<ObjectNode> response = connection.getClient().search(request, ObjectNode.class);
                Optional<Hit<ObjectNode>> money = response.hits().hits().stream().findFirst();
                if (money.isPresent() && money.get().source() != null && money.get().source().has("amount")) {
                    return money.get().source().get("amount").asInt();
                }
                return 0;
            } catch (ElasticsearchException | IOException exception) {
                throw new StorageExecuteException(exception, "Error while executing search request");
            }
        } catch (IOException exception) {
            throw new StorageExecuteException(exception, "Elasticsearch client init error.");
        }
    }

    @Override
    public @NotNull UUID addMoneyToTags(@NotNull Map<String, Object> tags,
                                        int amount, @Nullable String reason) throws StorageExecuteException {
        if (amount==0) {
            throw new StorageExecuteException(new Throwable(), "Amount can't be 0.");
        }
        return addOperation(tags, amount, reason);
    }

    @Override
    public @NotNull UUID setMoneyToTag(@NotNull String tagName, @NotNull Object tagValue,
                                        int amount, @Nullable String reason) throws StorageExecuteException {
        int calculatedAmount = amount - getMoneyFromTag(tagName, tagValue);
        return addOperation(Map.of(tagName, tagValue), calculatedAmount, reason);
    }

    private @NotNull UUID addOperation(@NotNull Map<String, Object> tags, int amount,
                               @Nullable String reason) throws StorageExecuteException {
        UUID transactionId = UUID.randomUUID();
        reason = Objects.requireNonNullElse(reason, "No reason provided");
        if (reason.isBlank()) reason = "No reason provided";
        Map<String, Object> log = new HashMap<>();
        log.put("transactionId", transactionId);
        log.put("tags", tags);
        log.put("operation", amount);
        log.put("reason", reason);
        log.put("@timestamp", DateMileKat.getDateEs());
        MoneyPrepareOperation event = new MoneyPrepareOperation(transactionId, tags, amount, reason);
        if (event.isCancelled()) {
            throw new StorageExecuteException(new Throwable(), "Money operation cancelled by plugin.");
        }
        moneyOperations.put(transactionId,
                new BulkOperation.Builder().create(
                        new CreateOperation.Builder<>()
                                .index(BANK_INDEX_TRANSACTIONS)
                                .document(log)
                                .build()
                ).build()
        );
        Main.getInstance().getServer().getPluginManager().callEvent(
                new MoneySavedSuccessfully(transactionId, tags, amount, reason));
        return transactionId;
    }

    private void saveOperation() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), ()-> {
            Map<UUID, BulkOperation> processing = new HashMap<>(moneyOperations);
            moneyOperations.clear();
            if (!processing.isEmpty()) {
                try (ESConnection connection = DB.getConnection()) {
                    try {
                        connection.getClient().bulk(
                                new BulkRequest.Builder()
                                        .operations(processing.values().stream().toList())
                                        .build()
                        );
                        Main.debug("'" + processing.size() + "' money operation(s) saved.");
                    } catch (ElasticsearchException | IOException exception) {
                        moneyOperations.putAll(processing);
                        Main.warning("Error while trying to save money operation(s).");
                        Main.stack(exception.getStackTrace());
                    }
                } catch (IOException exception) {
                    Main.warning("ElasticSearch client error.");
                    Main.stack(exception.getStackTrace());
                }
            }
        }, 50, 20);
    }
}
