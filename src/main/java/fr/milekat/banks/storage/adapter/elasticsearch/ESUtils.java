package fr.milekat.banks.storage.adapter.elasticsearch;

import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.PropertyBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ESUtils {
    public static @NotNull Map<String, Property> getMapping(@NotNull Map<String, Class<?>> fields,
                                                            @NotNull Map<String, Class<?>> tags,
                                                            @NotNull String tagsFieldName) {
        Map<String, Property> mapping = new HashMap<>();
        fields.forEach((field, type) -> mapping.putAll(getMapping(field, type)));
        Map<String, Property> tagsMapping = new HashMap<>();
        tags.forEach((field, type) -> tagsMapping.putAll(getMapping(field, type)));
        if (!tagsMapping.isEmpty()) {
            mapping.put(tagsFieldName, new Property(PropertyBuilders.object().properties(tagsMapping).build()));
        }
        return mapping;
    }

    private static @NotNull Map<String, Property> getMapping(@NotNull String field, @NotNull Class<?> type) {
        Map<String, Property> mapping = new HashMap<>();
        if (type.equals(String.class)) {
            mapping.put(field, new Property(PropertyBuilders.keyword().ignoreAbove(256).build()));
        } else if (type.equals(UUID.class)) {
            mapping.put(field, new Property(PropertyBuilders.keyword().ignoreAbove(36).build()));
        } else if (type.equals(Integer.class)) {
            mapping.put(field, new Property(PropertyBuilders.integer().build()));
        } else if (type.equals(Float.class)) {
            mapping.put(field, new Property(PropertyBuilders.keyword().build()));
        } else if (type.equals(Double.class)) {
            mapping.put(field, new Property(PropertyBuilders.double_().build()));
        } else if (type.equals(Boolean.class)) {
            mapping.put(field, new Property(PropertyBuilders.boolean_().build()));
        } else if (type.equals(Date.class)) {
            mapping.put(field, new Property(PropertyBuilders.date().build()));
        } else {
            mapping.put(field, new Property(PropertyBuilders.object().build()));
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
