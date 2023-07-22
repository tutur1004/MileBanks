package fr.milekat.societe.storage.adapter.elasticsearch;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.PropertyBuilders;
import co.elastic.clients.elasticsearch.transform.PivotGroupBy;
import fr.milekat.societe.Main;
import org.jetbrains.annotations.NotNull;

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

    public static void ensureAccountsAgg(@NotNull String PREFIX, @NotNull ESConnection DB,
                                         @NotNull Map<Class<?>, List<String>> tagsFormats) {
        Main.debug("Check if transform 'accounts' is present...");
        DB.getAsyncClient()
                .transform()
                .getTransform(t -> t.transformId("accounts"))
                .whenComplete((getTransformResponse, throwable) -> {
                    if (throwable!=null || getTransformResponse.count() == 0) {
                        Main.debug("Transform 'accounts' is not present !");
                        createAccountAgg(PREFIX, DB, tagsFormats);
                    } else {
                        Main.debug("Transform 'accounts' is present !");
                        startAccountAgg(DB);
                    }
                });
    }

    private static void createAccountAgg(@NotNull String PREFIX, @NotNull ESConnection DB,
                                  @NotNull Map<Class<?>, List<String>> tagsFormats) {
        Main.debug("Transform 'accounts' creating...");
        // TODO: 19/07/2023 Ensure index 'PREFIX + "accounts"' is not present ? To prevent error when create transform
        DB.getAsyncClient()
                .transform()
                .putTransform(t -> t
                                .source(s -> s.index(PREFIX+ "operations"))
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
                        ).whenComplete((putTransformResponse, throwable) -> {
                            if (throwable!=null) {
                                Main.debug("Transform create error: " + throwable.getMessage());
                                Main.stack(throwable.getStackTrace());
                                return;
                            }
                            Main.debug("Transform 'accounts' created !");
                            startAccountAgg(DB);
                        });
    }

    private static void startAccountAgg(@NotNull ESConnection DB) {
        Main.debug("Check if transform 'accounts' is started...");
        DB.getAsyncClient()
                .transform()
                .getTransformStats(t -> t.transformId("accounts"))
                .whenComplete((getStatsResponse, getStatsThrowable) -> {
                    if (getStatsThrowable!=null) {
                        Main.debug("Transform get stats error: " + getStatsThrowable.getMessage());
                        Main.stack(getStatsThrowable.getStackTrace());
                        return;
                    }
                    if (!getStatsResponse.transforms().get(0).state().equalsIgnoreCase("started")) {
                        Main.debug("Transform 'accounts' is not started, starting...");
                        DB.getAsyncClient()
                                .transform()
                                .startTransform(t -> t.transformId("accounts"))
                                .whenComplete((startTransformResponse, startThrowable) -> {
                                    if (startThrowable!=null) {
                                        Main.debug("Transform start error: " + startThrowable.getMessage());
                                        Main.stack(startThrowable.getStackTrace());
                                        return;
                                    }
                                    Main.debug("Transform 'accounts' started !");
                                });
                    } else {
                        Main.debug("Transform 'accounts' is started !");
                    }
                });
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
