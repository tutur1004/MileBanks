package fr.milekat.banks.storage.adapter.elasticsearch;

import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.PropertyBuilders;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ESUtils {
    public static @NotNull Map<String, Property> getTagsMapping(@NotNull Map<Class<?>, List<String>> tagsFormats) {
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
}
