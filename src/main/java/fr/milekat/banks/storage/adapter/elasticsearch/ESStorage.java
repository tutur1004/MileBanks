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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ESStorage implements StorageImplementation {
    // Elasticsearch connection
    private final ESConnection connection;
    private final String numberOfReplicas;

    // Index settings
    private final String BANK_INDEX_TRANSACTIONS;
    private final Map<String, Class<?>> transactions_fields = new HashMap<>();
    private final String BANK_INDEX_ACCOUNTS;
    private final Map<String, Class<?>> accounts_fields = new HashMap<>();

    // Thread-safe map for concurrent access
    private final Map<UUID, BulkOperation> moneyOperations = new ConcurrentHashMap<>();
    // No lock needed - ConcurrentHashMap handles concurrency

    // Scheduler tasks
    private BukkitTask saveTask;
    private BukkitTask healthCheckTask;

    // Connection health monitoring
    private volatile boolean isConnected = true;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicBoolean isReconnecting = new AtomicBoolean(false);
    private static final int MAX_CONSECUTIVE_FAILURES = 3;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    // Memory limit
    private final int MAX_PENDING_OPERATIONS;

    // Intervals in ticks
    private final long SAVE_INTERVAL_TICKS;
    private static final long HEALTH_CHECK_INTERVAL = 100L; // 5 seconds

    // Singleton mapper to avoid recreating client
    private final JacksonJsonpMapper mapper;

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
        this.MAX_PENDING_OPERATIONS = config.getInt("storage.elasticsearch.max-pending-operations", 10000);

        // Initialize singleton mapper
        this.mapper = createMapper();

        transactions_fields.put("operation", Double.class);
        transactions_fields.put("reason", String.class);
        transactions_fields.put("transactionId", UUID.class);
        transactions_fields.put("@timestamp", Date.class);

        accounts_fields.put("amount", Integer.class);
        accounts_fields.putAll(Main.TAGS);

        try {
            ElasticsearchClient esClient = connection.getEsClient(mapper);
            Main.getMileLogger().debug(esClient.cluster().health().toString());

            startHealthCheck();
            startSaveOperation();
        } catch (IOException exception) {
            Main.getMileLogger().stack(exception.getStackTrace());
            throw new StorageLoadException("Error while trying to load ElasticSearch cluster");
        }
    }

    @Contract(" -> new")
    private @NotNull JacksonJsonpMapper createMapper() {
        JacksonJsonpMapper newMapper = new JacksonJsonpMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(Date.class, new DateSerializer());
        module.addDeserializer(Date.class, new DateDeserializer());
        newMapper.objectMapper().registerModule(module);
        return newMapper;
    }

    @Override
    public boolean checkStorages() {
        Main.getMileLogger().debug("Check if storage is ready...");
        String TAGS_FIELD = "tags";
        try {
            ElasticsearchClient esClient = connection.getEsClient(mapper);
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
        } catch (StorageLoadException exception) {
            Main.getMileLogger().warning("ElasticSearch load storage error.");
            Main.getMileLogger().stack(exception.getStackTrace());
        }
        return false;
    }

    @Override
    public void disconnect() {
        Main.getMileLogger().info("Disconnecting from storage...");

        // Cancel scheduled tasks
        if (healthCheckTask != null && !healthCheckTask.isCancelled()) {
            healthCheckTask.cancel();
        }
        if (saveTask != null && !saveTask.isCancelled()) {
            saveTask.cancel();
        }

        // Flush remaining operations synchronously
        Main.getMileLogger().info("Flushing remaining operations...");
        flushMoneyOperations();

        connection.close();
        Main.getMileLogger().info("Storage disconnected.");
    }

    /**
     * Starts the periodic health check routine
     */
    private void startHealthCheck() {
        this.healthCheckTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                Main.getInstance(),
                this::checkConnection,
                HEALTH_CHECK_INTERVAL,
                HEALTH_CHECK_INTERVAL
        );
    }

    /**
     * Checks Elasticsearch connection health and attempts reconnection if needed
     */
    private void checkConnection() {
        try {
            ElasticsearchClient esClient = connection.getEsClient(mapper);
            esClient.cluster().health();

            if (!isConnected) {
                Main.getMileLogger().info("Connection to Elasticsearch restored!");
                isConnected = true;
                consecutiveFailures.set(0);
                isReconnecting.set(false);
            }
        } catch (IOException | ElasticsearchException exception) {
            int failures = consecutiveFailures.incrementAndGet();

            if (isConnected) {
                Main.getMileLogger().warning("Lost connection to Elasticsearch (attempt " +
                        failures + "/" + MAX_CONSECUTIVE_FAILURES + ")");
            }

            if (failures >= MAX_CONSECUTIVE_FAILURES && !isReconnecting.get()) {
                isConnected = false;
                attemptReconnection();
            }
        }
    }

    /**
     * Attempts to reconnect to Elasticsearch with thread safety
     */
    private void attemptReconnection() {
        if (!isReconnecting.compareAndSet(false, true)) {
            return; // Another thread is already reconnecting
        }

        try {
            Main.getMileLogger().severe("Elasticsearch connection lost! Attempting reconnection...");
            connection.reconnect();
            Main.getMileLogger().info("Reconnection successful!");
            isConnected = true;
            consecutiveFailures.set(0);
        } catch (Exception e) {
            Main.getMileLogger().severe("Reconnection failed: " + e.getMessage());
        } finally {
            isReconnecting.set(false);
        }
    }

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

    /**
     * Fetches money with automatic retry on failure
     */
    private int fetchMoney(@NotNull SearchRequest request) throws StorageExecuteException {
        int attempts = 0;
        StorageExecuteException lastException = null;

        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                ElasticsearchClient esClient = connection.getEsClient(mapper);
                SearchResponse<ObjectNode> response = esClient.search(request, ObjectNode.class);
                Optional<Hit<ObjectNode>> money = response.hits().hits().stream().findFirst();
                if (money.isPresent() && money.get().source() != null && money.get().source().has("amount")) {
                    return money.get().source().get("amount").asInt();
                }
                return 0;
            } catch (ElasticsearchException | IOException e) {
                lastException = new StorageExecuteException(e, "Error fetching money from Elasticsearch");
                attempts++;

                if (attempts < MAX_RETRY_ATTEMPTS) {
                    Main.getMileLogger().warning("Fetch failed (attempt " + attempts +
                            "/" + MAX_RETRY_ATTEMPTS + "), retrying...");

                    // Wait before retry
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        throw lastException;
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
                                       @Nullable String reason) throws StorageExecuteException {
        // Quick check without lock - trigger async flush if near limit
        int currentSize = moneyOperations.size();
        if (currentSize >= MAX_PENDING_OPERATIONS) {
            Main.getMileLogger().warning("Storage buffer full (" + currentSize +
                    "/" + MAX_PENDING_OPERATIONS + "). Triggering immediate async flush.");

            // Trigger flush asynchronously without blocking
            Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), this::flushMoneyOperations);

            // Still accept the operation - it will be in the next flush
            // Only reject if REALLY over limit (safety margin)
            if (currentSize >= MAX_PENDING_OPERATIONS * 2) {
                Main.getMileLogger().severe("Storage buffer critically full (" + currentSize +
                        "). Rejecting operation to prevent memory overflow.");
                throw new StorageExecuteException(
                        new Throwable("Storage buffer critically full"),
                        "Cannot accept new operations: buffer is critically full"
                );
            }
        }

        UUID transactionId = UUID.randomUUID();
        String finalReason = (reason == null || reason.isBlank()) ? "No reason provided" : reason;

        Map<String, Object> log = new HashMap<>();
        log.put("transactionId", transactionId);
        log.put("tags", tags);
        log.put("operation", amount);
        log.put("reason", finalReason);
        log.put("@timestamp", DateMileKat.getDateEs());

        BulkOperation operation = new BulkOperation.Builder().create(
                new CreateOperation.Builder<>()
                        .index(BANK_INDEX_TRANSACTIONS)
                        .document(log)
                        .build()
        ).build();
        moneyOperations.put(transactionId, operation);

        // Fire event on main thread (non-blocking)
        Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                Bukkit.getPluginManager().callEvent(
                        new MoneySavedSuccessfully(transactionId, tags, amount, finalReason)
                )
        );

        return transactionId;
    }

    /**
     * Starts the periodic save operation routine
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
     * Lock-free design using ConcurrentHashMap for maximum performance
     */
    private void flushMoneyOperations() {
        if (moneyOperations.isEmpty()) {
            return;
        }

        // Atomic swap: create new empty map and get reference to old one
        Map<UUID, BulkOperation> processing = new ConcurrentHashMap<>();

        // Process all current operations
        moneyOperations.forEach((uuid, operation) -> {
            processing.put(uuid, operation);
            moneyOperations.remove(uuid);
        });

        // Safety check after swap
        if (processing.isEmpty()) {
            return;
        }

        try {
            ElasticsearchClient esClient = connection.getEsClient(mapper);
            BulkResponse response = esClient.bulk(
                    new BulkRequest.Builder()
                            .operations(new ArrayList<>(processing.values()))
                            .build()
            );

            // Successful bulk operation - reset failure counter
            int previousFailures = consecutiveFailures.getAndSet(0);
            if (previousFailures > 0) {
                Main.getMileLogger().info("Flush successful after " + previousFailures + " failures");
                isConnected = true;
            }

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
            int failures = consecutiveFailures.incrementAndGet();
            if (failures >= MAX_CONSECUTIVE_FAILURES) {
                isConnected = false;
            }
            Main.getMileLogger().warning("Error while trying to save money operation(s): " +
                    exception.getMessage());
            Main.getMileLogger().stack(exception.getStackTrace());
        }
    }
}