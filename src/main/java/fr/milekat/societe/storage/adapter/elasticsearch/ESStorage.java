package fr.milekat.societe.storage.adapter.elasticsearch;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.CreateOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.milekat.societe.Main;
import fr.milekat.societe.storage.StorageImplementation;
import fr.milekat.societe.storage.exceptions.StorageExecuteException;
import fr.milekat.societe.storage.exceptions.StorageLoaderException;
import fr.milekat.utils.Configs;
import fr.milekat.utils.DateMileKat;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

public class ESStorage implements StorageImplementation {
    private final String PREFIX;
    private final String numberOfReplicas;
    private final Map<Class<?>, List<String>> tagsFormats = new HashMap<>();
    private final boolean allowEmptyTags;
    private final ESConnection DB;
    private final Map<UUID, BulkOperation> moneyChanges = new HashMap<>();

    /*
        Main DB
    */
    public ESStorage(@NotNull Configs config) throws StorageLoaderException {
        this.PREFIX = config.getString("storage.elasticsearch.prefix", "bank-");
        this.numberOfReplicas = config.getString("storage.elasticsearch.replicas", "0");
        if (config.getBoolean("default_tags", true)) {
            tagsFormats.put(String.class, Arrays.asList("name", "uuid"));
        } else {
            tagsFormats.put(String.class, config.getStringList("custom_tags.string"));
            tagsFormats.put(Integer.class, config.getStringList("custom_tags.integer"));
            tagsFormats.put(Float.class, config.getStringList("custom_tags.long"));
            tagsFormats.put(Double.class, config.getStringList("custom_tags.double"));
            tagsFormats.put(Boolean.class, config.getStringList("custom_tags.boolean"));
        }
        this.allowEmptyTags = config.getBoolean("allow_empty_tags", false);
        try {
            DB = new ESConnection(config);
            Main.debug(DB.getClient().cluster().health().toString());
            saveOperation();
        } catch (IOException e) {
            throw new StorageLoaderException("Error while trying to load ElasticSearch cluster");
        }
    }

    @Override
    public boolean checkStorages() {
        String index = "operations";
        //  Check if index exist, otherwise create it
        Main.debug("Check if index '" + PREFIX + index + "' is present...");
        DB.getAsyncClient()
                .indices()
                .exists(e -> e.index(PREFIX + index))
                .whenComplete((booleanResponse, ignored) -> {
                    if (!booleanResponse.value()) {
                        Main.debug("Index '" + PREFIX + index +"' not found, creating...");
                        DB.getAsyncClient()
                                .indices()
                                .create(c -> c.index(PREFIX + index)
                                        .mappings(m -> m.properties(ESUtils.getMapping(tagsFormats)))
                                        .settings(s -> s.numberOfReplicas(numberOfReplicas))
                                ).whenComplete((createIndexResponse, createIndexthrowable) -> {
                                    if (createIndexthrowable != null) {
                                        Main.debug("Index create error: " + createIndexthrowable.getMessage());
                                        Main.stack(createIndexthrowable.getStackTrace());
                                        return;
                                    }
                                    Main.debug("Index '" + createIndexResponse.index() + "' created !");
                                    ESUtils.ensureAccountsAgg(PREFIX, DB, tagsFormats);
                                });
                    } else {
                        Main.debug("Index '" + PREFIX + index +"' found !");
                        ESUtils.ensureAccountsAgg(PREFIX, DB, tagsFormats);
                    }
                });
        return true;
    }

    @Override
    public String getImplementationName() {
        return null;
    }

    @Override
    public void disconnect() {
        try {
            DB.close();
            Main.debug("ElasticSearch connection closed.");
        } catch (IOException e) {
            Main.warning("Error while trying to close RestClient for Elasticsearch connection.");
        }
    }

    /*
        ES Queries execution
     */

    @Override
    public int getMoney(@NotNull UUID player) throws StorageExecuteException {
        try {
            Main.debug("[ES-Sync] getMoney - search money with uuid '" + player + "'.");
            SearchRequest request = new SearchRequest.Builder()
                            .index(PREFIX + "accounts")
                            .query(q -> q.match(m -> m.query("uuid").field(player.toString())))
                            .size(1)
                            .build();
            return fetchMoney(request);
        } catch (IOException e) {
            throw new StorageExecuteException(e, "Error while trying to fetch money for uuid " + player);
        }
    }

    @Override
    public int getTagsMoney(@NotNull Map<String, Object> tags) throws StorageExecuteException {
        try {
            Main.debug("[ES-Sync] getTagsMoney - search money with tags '" + tags + "'.");
            BoolQuery.Builder boolQuery = new BoolQuery.Builder();
            for (Map.Entry<String, Object> entry : tags.entrySet()) {
                if (entry.getValue() instanceof String value) {
                    boolQuery.must(mu -> mu.match(ma -> ma.field(entry.getKey()).query(value)));
                } else if (entry.getValue() instanceof Boolean value) {
                    boolQuery.must(mu -> mu.match(ma -> ma.field(entry.getKey()).query(value)));
                } else if (entry.getValue() instanceof Integer value) {
                    boolQuery.must(mu -> mu.match(ma -> ma.field(entry.getKey()).query(value)));
                } else if (entry.getValue() instanceof Long value) {
                    boolQuery.must(mu -> mu.match(ma -> ma.field(entry.getKey()).query(value)));
                } else if (entry.getValue() instanceof Double value) {
                    boolQuery.must(mu -> mu.match(ma -> ma.field(entry.getKey()).query(value)));
                }
            }
            SearchRequest request = new SearchRequest.Builder()
                            .index(PREFIX + "accounts")
                            .query(q -> q.bool(boolQuery.build()))
                            .size(1)
                            .build();
            return fetchMoney(request);
        } catch (IOException e) {
            throw new StorageExecuteException(e, "Error while trying to fetch money for tags " + tags);
        }
    }

    private int fetchMoney(@NotNull SearchRequest request) throws IOException {
        Main.debug("Request: " + request);

        SearchResponse<ObjectNode> response = DB.getClient().search(request, ObjectNode.class);
        Optional<Hit<ObjectNode>> money = response.hits().hits().stream().findFirst();
        if (money.isPresent() && money.get().source() != null && money.get().source().has("amount")) {
            return money.get().source().get("amount").asInt();
        }
        return 0;
    }

    @Override
    public @NotNull UUID addMoneyToTags(@NotNull UUID player, @NotNull Map<String, Object> tags,
                                        int amount, @Nullable String reason) throws StorageExecuteException {
        if (amount==0) {
            throw new StorageExecuteException(new Throwable(), "Amount can't be 0.");
        }
        return addOperation(player, tags, amount, reason);
    }

    @Override
    public @NotNull UUID setMoneyToTags(@NotNull UUID player, @NotNull Map<String, Object> tags,
                                        int amount, @Nullable String reason) throws StorageExecuteException {
        int calculatedAmount = amount - getTagsMoney(tags);
        return addOperation(player, tags, calculatedAmount, reason);
    }

    private @NotNull UUID addOperation(@NotNull UUID player, @NotNull Map<String, Object> tags, int amount,
                               @Nullable String reason) throws StorageExecuteException {
        if (!allowEmptyTags && tags.size() == 0) {
            throw new StorageExecuteException(new IllegalArgumentException("Empty tags not allowed."),
                    "Empty tags can't be saved in storage, because 'allow_empty_tags' config is set to false. " +
                            "Ensure you correctly define tags for UUID '" + player + "'.");
        }
        UUID transactionId = UUID.randomUUID();
        reason = Objects.requireNonNullElse(reason, "No reason provided");
        if (reason.isBlank()) reason = "No reason provided";
        Map<String, Object> log = new HashMap<>();
        log.put("transactionId", transactionId);
        log.put("uuid", player);
        log.put("tags", tags);
        log.put("operation", amount);
        log.put("reason", reason);
        log.put("@timestamp", DateMileKat.getDateEs());
        moneyChanges.put(transactionId,
                new BulkOperation.Builder().create(
                        new CreateOperation.Builder<>()
                                .index(PREFIX + "operations")
                                .document(log)
                                .build()
                ).build()
        );
        return transactionId;
    }

    private void saveOperation() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), ()-> {
            Map<UUID, BulkOperation> processing = new HashMap<>(moneyChanges);
            moneyChanges.clear();
            if (processing.size() > 0) {
                DB.getAsyncClient().bulk(
                        new BulkRequest.Builder()
                                .operations(processing.values().stream().toList())
                                .build()
                ).whenComplete((bulkResponse, bulkException) -> {
                    if (bulkResponse.errors() && bulkException!=null) {
                        moneyChanges.putAll(processing);
                        Main.warning("Error while trying to save money changes.");
                        Main.stack(bulkException.getStackTrace());
                    } else {
                        Main.debug("'" + processing.size() + "' money changes saved.");
                    }
                });
            }
        }, 50, 20);
    }
}
