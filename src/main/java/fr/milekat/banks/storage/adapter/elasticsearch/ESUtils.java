package fr.milekat.banks.storage.adapter.elasticsearch;

import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.PropertyBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ESUtils {
    public static @NotNull Map<String, Property> getTagsMapping(@NotNull Map<String, Class<?>> tagsFormats) {
        Map<String, Property> mapping = new HashMap<>();
        mapping.put("@timestamp", new Property(PropertyBuilders.date().build()));
        mapping.put("operation", new Property(PropertyBuilders.double_().build()));
        mapping.put("reason", new Property(PropertyBuilders.text().build()));
        mapping.put("transactionId", new Property(PropertyBuilders.keyword().ignoreAbove(36).build()));
        mapping.put("uuid", new Property(PropertyBuilders.keyword().ignoreAbove(36).build()));
        Map<String, Property> tags = new HashMap<>();
        tagsFormats.forEach((field, type) -> {
            if (type.equals(String.class)) {
                tags.put(field, new Property(PropertyBuilders.keyword().ignoreAbove(256).build()));
            } else if (type.equals(Integer.class)) {
                tags.put(field, new Property(PropertyBuilders.integer().build()));
            } else if (type.equals(Float.class)) {
                tags.put(field, new Property(PropertyBuilders.keyword().build()));
            } else if (type.equals(Double.class)) {
                tags.put(field, new Property(PropertyBuilders.double_().build()));
            } else if (type.equals(Boolean.class)) {
                tags.put(field, new Property(PropertyBuilders.boolean_().build()));
            }
        });
        if (!tags.isEmpty()) {
            mapping.put("tags", new Property(PropertyBuilders.object().properties(tags).build()));
        }
        return mapping;
    }

    @NotNull
    public static BoolQuery.Builder getBuilder(@NotNull String tagName, @NotNull Object tagValue) {
        BoolQuery.Builder boolQuery = new BoolQuery.Builder();
        if (tagValue instanceof String value) {
            boolQuery.must(mu -> mu.match(ma -> ma.field(tagName).query(value)));
        } else if (tagValue instanceof Boolean value) {
            boolQuery.must(mu -> mu.match(ma -> ma.field(tagName).query(value)));
        } else if (tagValue instanceof Integer value) {
            boolQuery.must(mu -> mu.match(ma -> ma.field(tagName).query(value)));
        } else if (tagValue instanceof Long value) {
            boolQuery.must(mu -> mu.match(ma -> ma.field(tagName).query(value)));
        } else if (tagValue instanceof Double value) {
            boolQuery.must(mu -> mu.match(ma -> ma.field(tagName).query(value)));
        }
        return boolQuery;
    }
}
