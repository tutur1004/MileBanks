package fr.milekat.banks.storage.adapter.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.PropertyBuilders;
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
                        new Property(PropertyBuilders.keyword().build())));
            } else if (type.equals(Double.class)) {
                fields.forEach(field -> tags.put(field,
                        new Property(PropertyBuilders.double_().build())));
            } else if (type.equals(Boolean.class)) {
                fields.forEach(field -> tags.put(field,
                        new Property(PropertyBuilders.boolean_().build())));
            }
        });
        if (!tags.isEmpty()) {
            mapping.put("tags", new Property(PropertyBuilders.object().properties(tags).build()));
        }
        return mapping;
    }

    public static void ensureAccountsAgg(@NotNull ElasticsearchClient client,
                                         @NotNull String indexSource,
                                         @NotNull String indexAgg,
                                         @NotNull Map<Class<?>, List<String>> tagsFormats)
            throws StorageExecuteException {
        try {
            Main.debug("Check if transform '" + indexAgg + "' is present...");
            client.transform().getTransform(t -> t.transformId(indexAgg));
            Main.debug("Transform '" + indexAgg + "' is present !");
        } catch (ElasticsearchException | IOException exception) {
            Main.debug("Transform '" + indexAgg + "' is not present !");
            createAccountAgg(client, indexSource, indexAgg, tagsFormats);
        } finally {
            startAccountAgg(client, indexAgg);
        }
    }

    private static void createAccountAgg(@NotNull ElasticsearchClient client,
                                          @NotNull String indexSource,
                                          @NotNull String indexAgg,
                                          @NotNull Map<Class<?>, List<String>> tagsFormats)
            throws StorageExecuteException {
        try {
            Main.debug("Transform '" + indexAgg + "' creating...");
            client.transform()
                    .putTransform(t -> t
                            .source(s -> s.index(indexSource))
                            .pivot(p -> p
                                    .groupBy(ESUtils.getMappingPivotGroups(tagsFormats))
                                    .aggregations("amount", new Aggregation.Builder()
                                            .sum(s -> s.field("operation"))
                                            .build()
                                    )
                            )
                            .transformId(indexAgg)
                            .description("Accounts aggregation based on '" + indexSource + "'")
                            .dest(d -> d.index(indexAgg))
                            .sync(s -> s.time(st -> st.field("@timestamp").delay(d -> d.time("3s"))))
                            .frequency(f -> f.time("2s"))
                    );
            Main.debug("Transform '" + indexAgg + "' created !");
        } catch (ElasticsearchException | IOException exception) {
            throw new StorageExecuteException(exception, "Transform create error: " + exception.getMessage());
        }
    }

    private static void startAccountAgg(@NotNull ElasticsearchClient client, @NotNull String indexAgg)
            throws StorageExecuteException {
        try {
            Main.debug("Check if transform '" + indexAgg + "' is started...");
            GetTransformStatsResponse getStatsResponse = client.transform()
                    .getTransformStats(t -> t.transformId(indexAgg));
            if (!getStatsResponse.transforms().get(0).state().equalsIgnoreCase("started")) {
                try {
                    Main.debug("Transform '" + indexAgg + "' is not started, starting...");
                    client.transform().startTransform(t -> t.transformId(indexAgg));
                    Main.debug("Transform '" + indexAgg + "' started !");
                } catch (ElasticsearchException |IOException exception) {
                    throw new StorageExecuteException(exception, "Transform start error: " + exception.getMessage());
                }
            } else {
                Main.debug("Transform '" + indexAgg + "' is started !");
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
