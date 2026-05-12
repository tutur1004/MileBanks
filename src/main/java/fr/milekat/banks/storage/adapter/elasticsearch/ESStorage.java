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
import fr.milekat.banks.api.classes.BankAccount;
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

    // Index names
    private final String BANK_INDEX_TRANSACTIONS;
    private final String BANK_INDEX_TRANSACTIONS_ARCHIVED;
    private final String BANK_INDEX_ACCOUNTS;

    // Index field mappings
    private final Map<String, Class<?>> transactions_fields = new HashMap<>();
    private final Map<String, Class<?>> accounts_fields = new HashMap<>();

    // Pending bulk operations — lock-free via ConcurrentHashMap
    private final Map<UUID, BulkOperation> moneyOperations = new ConcurrentHashMap<>();

    // Scheduler tasks
    private BukkitTask saveTask;
    private BukkitTask healthCheckTask;

    // Connection health monitoring
    private volatile boolean isConnected = true;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicBoolean isReconnecting = new AtomicBoolean(false);
    private static final int MAX_CONSECUTIVE_FAILURES = 3;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    // Buffer overflow protection
    private final int MAX_PENDING_OPERATIONS;

    // Intervals (ticks)
    private final long SAVE_INTERVAL_TICKS;
    private static final long HEALTH_CHECK_INTERVAL = 100L; // 5 seconds

    // Singleton mapper — built once, reused across all calls
    private final JacksonJsonpMapper mapper;

    public ESStorage(@NotNull ESConnection connection, @NotNull Configs config) throws StorageLoadException {
        this.connection = connection;
        String prefix = config.getString("storage.elasticsearch.prefix", "bank-");
        if (!prefix.matches("[a-z0-9][a-z0-9-]{0,19}")) {
            throw new StorageLoadException("Elasticsearch prefix wrong, please only lower cases (a-z), " +
                    "digits (0-9) and dashes '-', also you can't start with a '-'.");
        }
        this.BANK_INDEX_TRANSACTIONS = prefix + "transactions";
        this.BANK_INDEX_TRANSACTIONS_ARCHIVED = BANK_INDEX_TRANSACTIONS + "-archived";
        this.BANK_INDEX_ACCOUNTS = prefix + "accounts";
        this.numberOfReplicas = config.getString("storage.elasticsearch.replicas", "0");
        this.SAVE_INTERVAL_TICKS = config.getLong("storage.elasticsearch.save-interval-ticks", 20L);
        this.MAX_PENDING_OPERATIONS = config.getInt("storage.elasticsearch.max-pending-operations", 1000);
        this.mapper = createMapper();

        transactions_fields.put("operation", Double.class);
        transactions_fields.put("reason", String.class);
        transactions_fields.put("transactionId", UUID.class);
        transactions_fields.put("@timestamp", Date.class);

        accounts_fields.put("amount", Integer.class);
        accounts_fields.putAll(Main.TAGS); // includes "currency" when multi-currency

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
            new Index(esClient, BANK_INDEX_TRANSACTIONS_ARCHIVED, numberOfReplicas,
                    transactions_fields, Main.TAGS, TAGS_FIELD);
            new Index(esClient, BANK_INDEX_ACCOUNTS, numberOfReplicas,
                    accounts_fields, new HashMap<>(), "");
            Main.getMileLogger().debug("Check transforms...");
            for (Map.Entry<String, Class<?>> tag : Main.TAGS.entrySet()) {
                // "currency" is combined with each player tag — no standalone transform
                if (tag.getKey().equals("currency")) continue;
                Map<String, Class<?>> groupBy = new LinkedHashMap<>();
                groupBy.put(tag.getKey(), tag.getValue());
                if (Main.isMultiCurrency()) {
                    groupBy.put("currency", String.class);
                }
                new Transforms(esClient,
                        BANK_INDEX_TRANSACTIONS, BANK_INDEX_ACCOUNTS,
                        "@timestamp", "0s", "1s",
                        groupBy);
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
        if (healthCheckTask != null && !healthCheckTask.isCancelled()) {
            healthCheckTask.cancel();
        }
        if (saveTask != null && !saveTask.isCancelled()) {
            saveTask.cancel();
        }
        Main.getMileLogger().info("Flushing remaining operations...");
        flushMoneyOperations();
        connection.close();
        Main.getMileLogger().info("Storage disconnected.");
    }

    // -------------------------------------------------------------------------
    // Health check
    // -------------------------------------------------------------------------

    private void startHealthCheck() {
        this.healthCheckTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                Main.getInstance(),
                this::checkConnection,
                HEALTH_CHECK_INTERVAL,
                HEALTH_CHECK_INTERVAL
        );
    }

    private void checkConnection() {
        try {
            connection.getEsClient(mapper).cluster().health();
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

    private void attemptReconnection() {
        if (!isReconnecting.compareAndSet(false, true)) return;
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

    // -------------------------------------------------------------------------
    // ES Queries
    // -------------------------------------------------------------------------

    @Override
    public int getMoneyFromTags(@NotNull Map<String, Object> tags) throws StorageExecuteException {
        Main.getMileLogger().debug("[ES-Sync] getMoneyFromTags - search money with tags " + tags + ".");
        if (tags.isEmpty()) return 0;
        BoolQuery.Builder boolQuery;
        if (tags.size() == 1) {
            Map.Entry<String, Object> entry = tags.entrySet().iterator().next();
            boolQuery = Builders.getBuilder(entry.getKey(), entry.getValue());
        } else {
            boolQuery = new BoolQuery.Builder();
            for (Map.Entry<String, Object> tag : tags.entrySet()) {
                String key = tag.getKey();
                String value = tag.getValue().toString();
                boolQuery.must(q -> q.term(t -> t.field(key).value(value)));
            }
        }
        SearchRequest request = new SearchRequest.Builder()
                .index(BANK_INDEX_ACCOUNTS)
                .query(q -> q.bool(boolQuery.build()))
                .size(1)
                .build();
        int balance = fetchMoney(request);
        CacheManager.addCacheAccount(Main.BANK_ACCOUNTS_CACHE, new BankAccount(tags, balance));
        return balance;
    }

    private int fetchMoney(@NotNull SearchRequest request) throws StorageExecuteException {
        StorageExecuteException lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                ElasticsearchClient esClient = connection.getEsClient(mapper);
                SearchResponse<ObjectNode> response = esClient.search(request, ObjectNode.class);
                Optional<Hit<ObjectNode>> hit = response.hits().hits().stream().findFirst();
                if (hit.isPresent() && hit.get().source() != null && hit.get().source().has("amount")) {
                    return hit.get().source().get("amount").asInt();
                }
                return 0;
            } catch (ElasticsearchException | IOException e) {
                lastException = new StorageExecuteException(e, "Error fetching money from Elasticsearch");
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    Main.getMileLogger().warning("Fetch failed (attempt " + attempt +
                            "/" + MAX_RETRY_ATTEMPTS + "), retrying...");
                    try { Thread.sleep(100L); } catch (InterruptedException ie) {
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
    public @NotNull UUID resetMoneyToTags(@NotNull Map<String, Object> tags,
                                          int amount, @Nullable String reason) throws StorageExecuteException {
        Main.getMileLogger().debug("[ES-Sync] resetMoneyToTags - reset money for tags " + tags
                + " to amount " + amount + ".");
        BoolQuery.Builder boolQuery;
        if (tags.size() == 1) {
            Map.Entry<String, Object> entry = tags.entrySet().iterator().next();
            boolQuery = Builders.getBuilder(entry.getKey(), entry.getValue());
        } else {
            boolQuery = new BoolQuery.Builder();
            for (Map.Entry<String, Object> tag : tags.entrySet()) {
                String key = tag.getKey();
                String value = tag.getValue().toString();
                boolQuery.must(q -> q.term(t -> t.field("tags." + key).value(value)));
            }
        }
        // Build once — shared between reindex and deleteByQuery
        BoolQuery builtQuery = boolQuery.build();
        try {
            ElasticsearchClient esClient = connection.getEsClient(mapper);
            flushMoneyOperations();
            esClient.reindex(r -> r
                    .source(s -> s
                            .index(BANK_INDEX_TRANSACTIONS)
                            .query(q -> q.bool(builtQuery))
                    )
                    .dest(d -> d.index(BANK_INDEX_TRANSACTIONS_ARCHIVED))
            );
            esClient.deleteByQuery(r -> r
                    .index(BANK_INDEX_TRANSACTIONS)
                    .query(q -> q.bool(builtQuery))
            );
            return addOperation(tags, amount, reason);
        } catch (ElasticsearchException | IOException exception) {
            throw new StorageExecuteException(exception, "Error while trying to reset transactions");
        }
    }

    private @NotNull UUID addOperation(@NotNull Map<String, Object> tags, int amount,
                                       @Nullable String reason) throws StorageExecuteException {
        int currentSize = moneyOperations.size();
        if (currentSize >= MAX_PENDING_OPERATIONS) {
            if (currentSize >= MAX_PENDING_OPERATIONS * 10) {
                Main.getMileLogger().severe("Storage buffer critically full (" + currentSize +
                        "). Rejecting operation to prevent memory overflow.");
                throw new StorageExecuteException(
                        new Throwable("Storage buffer critically full"),
                        "Cannot accept new operations: buffer is critically full"
                );
            }
            Main.getMileLogger().warning("Storage buffer full (" + currentSize +
                    "/" + MAX_PENDING_OPERATIONS + "). Triggering immediate async flush.");
            Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), this::flushMoneyOperations);
        }

        UUID transactionId = UUID.randomUUID();
        String finalReason = (reason == null || reason.isBlank()) ? "No reason provided" : reason;

        Map<String, Object> log = new HashMap<>();
        log.put("transactionId", transactionId);
        log.put("tags", tags);
        log.put("operation", amount);
        log.put("reason", finalReason);
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
        if (moneyOperations.isEmpty()) return;

        // Atomic drain: move all current entries into a local map
        Map<UUID, BulkOperation> processing = new ConcurrentHashMap<>();
        moneyOperations.forEach((uuid, op) -> {
            processing.put(uuid, op);
            moneyOperations.remove(uuid);
        });
        if (processing.isEmpty()) return;

        try {
            ElasticsearchClient esClient = connection.getEsClient(mapper);
            BulkResponse response = esClient.bulk(
                    new BulkRequest.Builder()
                            .operations(new ArrayList<>(processing.values()))
                            .build()
            );

            int previousFailures = consecutiveFailures.getAndSet(0);
            if (previousFailures > 0) {
                Main.getMileLogger().info("Flush successful after " + previousFailures + " failure(s).");
                isConnected = true;
            }

            if (response.errors()) {
                int failedCount = 0, successCount = 0;
                List<UUID> ids = new ArrayList<>(processing.keySet());
                for (int i = 0; i < response.items().size(); i++) {
                    BulkResponseItem item = response.items().get(i);
                    if (item.error() != null) {
                        moneyOperations.put(ids.get(i), processing.get(ids.get(i)));
                        failedCount++;
                        Main.getMileLogger().warning("Failed to save transaction " + ids.get(i) +
                                ": " + item.error().reason());
                    } else {
                        successCount++;
                    }
                }
                Main.getMileLogger().warning("Bulk completed with errors: " +
                        successCount + " succeeded, " + failedCount + " failed.");
            } else {
                Main.getMileLogger().debug("'" + processing.size() + "' money operation(s) saved successfully.");
            }
        } catch (ElasticsearchException | IOException exception) {
            moneyOperations.putAll(processing);
            int failures = consecutiveFailures.incrementAndGet();
            if (failures >= MAX_CONSECUTIVE_FAILURES) isConnected = false;
            Main.getMileLogger().warning("Error while trying to save money operation(s): " + exception.getMessage());
            Main.getMileLogger().stack(exception.getStackTrace());
        }
    }
}
