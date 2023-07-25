package fr.milekat.banks.storage.adapter.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.PropertyBuilders;
import co.elastic.clients.elasticsearch.transform.GetTransformResponse;
import co.elastic.clients.elasticsearch.transform.GetTransformStatsResponse;
import co.elastic.clients.elasticsearch.transform.PivotGroupBy;
import fr.milekat.banks.Main;
import fr.milekat.banks.storage.exceptions.StorageExecuteException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ESUtils {
    public static @NotNull Map<String, Property> getMapping(@NotNull Map<Class<?>, List<String>> tagsFormats) {
        Map<String, Property> mapping = new HashMap<>();
        mapping.put("@timestamp", new Property(PropertyBuilders.date().build()));
        mapping.put("operation", new Property(PropertyBuilders.double_().build()));
        mapping.put("reason", new Property(PropertyBuilders.text().build()));
        mapping.put("transactionId", new Property(PropertyBuilders.keyword().ignoreAbove(36).build()));
        mapping.put("uuid", new Property(PropertyBuilders.keyword().ignoreAbove(36).build()));
        Map<String, Property> tags = new HashMap<>();
        tagsFormats.forEach((type, fields) -> {
            if (type.equals(String.class)) {
                fields.forEach(field -> tags.put(field,
                        new Property(PropertyBuilders.keyword().ignoreAbove(256).build())));
            } else if (type.equals(Integer.class)) {
                fields.forEach(field -> tags.put(field,
                        new Property(PropertyBuilders.integer().build())));
            } else if (type.equals(Float.class)) {
                fields.forEach(field -> tags.put(field,
                        new Property(PropertyBuilders.long_().build())));
            } else if (type.equals(Double.class)) {
                fields.forEach(field -> tags.put(field,
                        new Property(PropertyBuilders.double_().build())));
            } else if (type.equals(Boolean.class)) {
                fields.forEach(field -> tags.put(field,
                        new Property(PropertyBuilders.boolean_().build())));
            }
        });
        if (tags.size()>0) {
            mapping.put("tags", new Property(PropertyBuilders.object().properties(tags).build()));
        }
        return mapping;
    }

    public static void ensureAccountsAgg(@NotNull String PREFIX, @NotNull ElasticsearchClient client,
                                         @NotNull Map<Class<?>, List<String>> tagsFormats)
            throws StorageExecuteException {
        try {
            Main.debug("Check if transform 'accounts' is present...");
            GetTransformResponse getTransformResponse = client
                    .transform()
                    .getTransform(t -> t.transformId("accounts"));
            if (getTransformResponse.count() == 0) {
                Main.debug("Transform 'accounts' is not present !");
                createAccountAgg(PREFIX, client, tagsFormats);
            } else {
                Main.debug("Transform 'accounts' is present !");
                startAccountAgg(client);
            }
        } catch (ElasticsearchException | IOException exception) {
            throw new StorageExecuteException(exception, "Error while trying to get ES transform 'accounts'");
        }
    }

    private static void createAccountAgg(@NotNull String PREFIX, @NotNull ElasticsearchClient client,
                                  @NotNull Map<Class<?>, List<String>> tagsFormats) throws StorageExecuteException {
        try {
            Main.debug("Transform 'accounts' creating...");
            // TODO: 19/07/2023 Ensure index 'PREFIX + "accounts"' is not present ?
            //  To prevent error when create transform
            client.transform()
                    .putTransform(t -> t
                            .source(s -> s.index(PREFIX + "operations"))
                            .pivot(p -> p
                                    .groupBy(ESUtils.getMappingPivotGroups(tagsFormats))
                                    .aggregations("amount", new Aggregation.Builder()
                                            .sum(s -> s.field("operation"))
                                            .build()
                                    )
                            )
                            .transformId("accounts")
                            .description("Accounts aggregation based on " + PREFIX + "operations")
                            .dest(d -> d.index(PREFIX + "accounts"))
                            .sync(s -> s.time(st -> st.field("@timestamp").delay(d -> d.time("0s"))))
                            .frequency(f -> f.time("1s"))
                    );
            Main.debug("Transform 'accounts' created !");
            startAccountAgg(client);
        } catch (ElasticsearchException | IOException exception) {
            throw new StorageExecuteException(exception, "Transform create error: " + exception.getMessage());
        }
    }

    private static void startAccountAgg(@NotNull ElasticsearchClient client) throws StorageExecuteException {
        try {
            Main.debug("Check if transform 'accounts' is started...");
            GetTransformStatsResponse getStatsResponse = client.transform()
                    .getTransformStats(t -> t.transformId("accounts"));
            if (!getStatsResponse.transforms().get(0).state().equalsIgnoreCase("started")) {
                try {
                    Main.debug("Transform 'accounts' is not started, starting...");
                    client.transform().startTransform(t -> t.transformId("accounts"));
                    Main.debug("Transform 'accounts' started !");
                } catch (ElasticsearchException |IOException exception) {
                    throw new StorageExecuteException(exception, "Transform start error: " + exception.getMessage());
                }
            } else {
                Main.debug("Transform 'accounts' is started !");
            }
        } catch (ElasticsearchException |IOException exception) {
            throw new StorageExecuteException(exception, "Transform get stats error: " + exception.getMessage());
        }
    }

    public static @NotNull Map<String, PivotGroupBy> getMappingPivotGroups(
            @NotNull Map<Class<?>, List<String>> tagsFormats) {
        Map<String, PivotGroupBy> pivotGroups = new HashMap<>();
        String B = "tags.";
        tagsFormats.forEach((type, fields) -> {
            if (type.equals(String.class)) {
                fields.forEach(field -> pivotGroups.put(field,
                        new PivotGroupBy.Builder().terms(t -> t.field(B+field)).build()));
            } else if (type.equals(Integer.class)) {
                fields.forEach(field -> pivotGroups.put(field,
                        new PivotGroupBy.Builder().terms(t -> t.field(B+field)).build()));
            } else if (type.equals(Float.class)) {
                fields.forEach(field -> pivotGroups.put(field,
                        new PivotGroupBy.Builder().terms(t -> t.field(B+field)).build()));
            } else if (type.equals(Double.class)) {
                fields.forEach(field -> pivotGroups.put(field,
                        new PivotGroupBy.Builder().terms(t -> t.field(B+field)).build()));
            } else if (type.equals(Boolean.class)) {
                fields.forEach(field -> pivotGroups.put(field,
                        new PivotGroupBy.Builder().terms(t -> t.field(B+field)).build()));
            }
        });
        return pivotGroups;
    }
}
