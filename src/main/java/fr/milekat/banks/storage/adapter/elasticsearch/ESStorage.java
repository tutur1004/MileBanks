package fr.milekat.banks.storage.adapter.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.CreateOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.milekat.banks.Main;
import fr.milekat.banks.api.events.MoneySavedSuccessfully;
import fr.milekat.banks.storage.CacheManager;
import fr.milekat.banks.storage.StorageImplementation;
import fr.milekat.banks.utils.BankAccount;
import fr.milekat.utils.Configs;
import fr.milekat.utils.DateMileKat;
import fr.milekat.utils.storage.adapter.elasticsearch.connection.ESConnection;
import fr.milekat.utils.storage.adapter.elasticsearch.features.Index;
import fr.milekat.utils.storage.adapter.elasticsearch.features.Transforms;
import fr.milekat.utils.storage.adapter.elasticsearch.mappers.milekat.utils.DateDeserializer;
import fr.milekat.utils.storage.adapter.elasticsearch.mappers.milekat.utils.DateSerializer;
import fr.milekat.utils.storage.adapter.elasticsearch.utils.Builders;
import fr.milekat.utils.storage.exceptions.StorageExecuteException;
import fr.milekat.utils.storage.exceptions.StorageLoadException;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ESStorage implements StorageImplementation {
    // Elastic settings
    private final ESConnection connection;
    private final String numberOfReplicas;

    // Indexes settings
    private final String BANK_INDEX_TRANSACTIONS;
    private final Map<String, Class<?>> transactions_fields = new HashMap<>();
    private final String BANK_INDEX_ACCOUNTS;
    private final Map<String, Class<?>> accounts_fields = new HashMap<>();

    // Thread-safe map for concurrent access
    private final Map<UUID, BulkOperation> moneyOperations = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock operationLock = new ReentrantReadWriteLock();

    // Scheduler task reference for cleanup
    private BukkitTask saveTask;

    // Save interval in ticks
    private final long SAVE_INTERVAL_TICKS;

    /*
        Main DB
    */
    public ESStorage(@NotNull ESConnection connection, @NotNull Configs config) throws StorageLoadException {
        this.connection = connection;
        String prefix = config.getString("storage.elasticsearch.prefix", "bank-");
        if (!prefix.matches("[a-z0-9][a-z0-9-]{0,19}")) {
            throw new StorageLoadException("Elasticsearch prefix wrong, please only lower cases (a-z), " +
                    "digits (0-9) and dashes '-', also you can't start with a '-'.");
        }
        this.BANK_INDEX_TRANSACTIONS = prefix + "transactions";
        this.BANK_INDEX_ACCOUNTS = prefix + "accounts";
        this.numberOfReplicas = config.getString("storage.elasticsearch.replicas", "0");
        this.SAVE_INTERVAL_TICKS = config.getLong("storage.elasticsearch.save-interval-ticks", 20L);

        transactions_fields.put("operation", Double.class);
        transactions_fields.put("reason", String.class);
        transactions_fields.put("transactionId", UUID.class);
        transactions_fields.put("@timestamp", Date.class);

        accounts_fields.put("amount", Integer.class);
        accounts_fields.putAll(Main.TAGS);

        try (ElasticsearchClient esClient = connection.getEsClient(getMapper())) {
            Main.getMileLogger().debug(esClient.cluster().health().toString());
            startSaveOperation();
        } catch (IOException exception) {
            Main.getMileLogger().stack(exception.getStackTrace());
            throw new StorageLoadException("Error while trying to load ElasticSearch cluster");
        }
    }

    @Contract(" -> new")
    private @NotNull JacksonJsonpMapper getMapper() {
        JacksonJsonpMapper mapper = new JacksonJsonpMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Date.class, new DateSerializer());
        module.addDeserializer(Date.class, new DateDeserializer());
        mapper.objectMapper().registerModule(module);
        return mapper;
    }

    @Override
    public boolean checkStorages() {
        Main.getMileLogger().debug("Check if storage is ready...");
        String TAGS_FIELD = "tags";
        try (ElasticsearchClient esClient = connection.getEsClient(getMapper())) {
            Main.getMileLogger().debug("Check indices...");
            new Index(esClient, BANK_INDEX_TRANSACTIONS, numberOfReplicas,
                    transactions_fields, Main.TAGS, TAGS_FIELD);
            new Index(esClient, BANK_INDEX_ACCOUNTS, numberOfReplicas,
                    accounts_fields, new HashMap<>(), "");
            Main.getMileLogger().debug("Check transforms...");
            for (Map.Entry<String, Class<?>> tag : Main.TAGS.entrySet()) {
                new Transforms(esClient,
                        BANK_INDEX_TRANSACTIONS, BANK_INDEX_ACCOUNTS,
                        "@timestamp", "0s", "1s",
                        Map.of(tag.getKey(), tag.getValue()));
            }
            Main.getMileLogger().debug("Storage is ready.");
            return true;
        } catch (StorageLoadException | IOException exception) {
            Main.getMileLogger().warning("ElasticSearch load storage error.");
            Main.getMileLogger().stack(exception.getStackTrace());
        }
        return false;
    }

    @Override
    public void disconnect() {
        Main.getMileLogger().info("Disconnecting from storage...");

        // Cancel scheduled task
        if (saveTask != null && !saveTask.isCancelled()) {
            saveTask.cancel();
        }

        // Flush remaining operations
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), this::flushMoneyOperations, 1L);

        connection.close();
        Main.getMileLogger().info("Storage disconnected.");
    }

    /*
        ES Queries execution
     */

    @Override
    public int getMoneyFromTag(@NotNull String tagName, @NotNull Object tagValue) throws StorageExecuteException {
        Main.getMileLogger().debug("[ES-Sync] getMoneyFromTag - search money with tag '" + tagName + "=" + tagValue + "'.");
        BoolQuery.Builder boolQuery = Builders.getBuilder(tagName, tagValue);
        SearchRequest request = new SearchRequest.Builder()
                .index(BANK_INDEX_ACCOUNTS)
                .query(q -> q.bool(boolQuery.build()))
                .size(1)
                .build();
        int balance = fetchMoney(request);
        CacheManager.addCacheAccount(Main.BANK_ACCOUNTS_CACHE, new BankAccount(tagName, tagValue, balance));
        return balance;
    }

    private int fetchMoney(@NotNull SearchRequest request) throws StorageExecuteException {
        try (ElasticsearchClient esClient = connection.getEsClient(getMapper())) {
            SearchResponse<ObjectNode> response = esClient.search(request, ObjectNode.class);
            Optional<Hit<ObjectNode>> money = response.hits().hits().stream().findFirst();
            if (money.isPresent() && money.get().source() != null && money.get().source().has("amount")) {
                return money.get().source().get("amount").asInt();
            }
            return 0;
        } catch (ElasticsearchException | IOException exception) {
            throw new StorageExecuteException(exception, "Error while executing search request");
        }
    }

    @Override
    public @NotNull UUID addMoneyToTags(@NotNull Map<String, Object> tags,
                                        int amount, @Nullable String reason) throws StorageExecuteException {
        if (amount == 0) {
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
                                       @Nullable String reason) {
        UUID transactionId = UUID.randomUUID();
        reason = Objects.requireNonNullElse(reason, "No reason provided");
        if (reason.isBlank()) reason = "No reason provided";

        Map<String, Object> log = new HashMap<>();
        log.put("transactionId", transactionId);
        log.put("tags", tags);
        log.put("operation", amount);
        log.put("reason", reason);
        log.put("@timestamp", DateMileKat.getDateEs());

        moneyOperations.put(transactionId,
                new BulkOperation.Builder().create(
                        new CreateOperation.Builder<>()
                                .index(BANK_INDEX_TRANSACTIONS)
                                .document(log)
                                .build()
                ).build()
        );

        // Fire event on main thread
        String finalReason = reason;
        Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                Bukkit.getPluginManager().callEvent(
                        new MoneySavedSuccessfully(transactionId, tags, amount, finalReason)
                )
        );

        return transactionId;
    }

    /**
     * Starts the periodic save operation routine using Bukkit's scheduler
     */
    private void startSaveOperation() {
        this.saveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                Main.getInstance(),
                this::flushMoneyOperations,
                50L, // Initial delay (2.5 seconds)
                SAVE_INTERVAL_TICKS
        );
    }

    /**
     * Flushes all pending money operations to Elasticsearch
     */
    private void flushMoneyOperations() {
        operationLock.writeLock().lock();
        try {
            if (moneyOperations.isEmpty()) {
                return;
            }

            // Create a copy and clear the original map
            Map<UUID, BulkOperation> processing = new HashMap<>(moneyOperations);
            moneyOperations.clear();

            try (ElasticsearchClient esClient = connection.getEsClient(getMapper())) {
                BulkResponse response = esClient.bulk(
                        new BulkRequest.Builder()
                                .operations(new ArrayList<>(processing.values()))
                                .build()
                );

                // Check for errors in the bulk response
                if (response.errors()) {
                    int failedCount = 0;
                    int successCount = 0;
                    List<UUID> processingIds = new ArrayList<>(processing.keySet());

                    for (int i = 0; i < response.items().size(); i++) {
                        BulkResponseItem item = response.items().get(i);
                        UUID operationId = processingIds.get(i);

                        if (item.error() != null) {
                            // Restore failed operation
                            moneyOperations.put(operationId, processing.get(operationId));
                            failedCount++;

                            Main.getMileLogger().warning("Failed to save transaction " + operationId +
                                    ": " + item.error().reason());
                        } else {
                            successCount++;
                        }
                    }

                    Main.getMileLogger().warning("Bulk operation completed with errors: " +
                            successCount + " succeeded, " + failedCount + " failed."
                    );
                } else {
                    Main.getMileLogger().debug("'" + processing.size() + "' money operation(s) saved successfully.");
                }
            } catch (ElasticsearchException | IOException exception) {
                // Restore all operations on connection/request failure
                moneyOperations.putAll(processing);
                Main.getMileLogger().warning("Error while trying to save money operation(s).");
                Main.getMileLogger().stack(exception.getStackTrace());
            }
        } finally {
            operationLock.writeLock().unlock();
        }
    }
}