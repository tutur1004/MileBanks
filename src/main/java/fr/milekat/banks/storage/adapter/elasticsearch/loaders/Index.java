package fr.milekat.banks.storage.adapter.elasticsearch.loaders;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import fr.milekat.banks.Main;
import fr.milekat.banks.storage.adapter.elasticsearch.ESUtils;
import fr.milekat.banks.storage.exceptions.StorageLoaderException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Index {
    private final ElasticsearchClient client;
    private final String indexName;
    private final String numberOfReplicas;
    private final Map<String, Class<?>> fields = new HashMap<>();
    private final Map<String, Class<?>> tags = new HashMap<>();
    private final String tagsFieldName;


    public Index(@NotNull ElasticsearchClient client,
                 @NotNull String indexName,
                 @NotNull String numberOfReplicas,
                 @NotNull Map<String, Class<?>> fields,
                 @NotNull Map<String, Class<?>> tags,
                 @NotNull String tagsFieldName) throws StorageLoaderException {
        this.client = client;
        this.indexName = indexName;
        this.numberOfReplicas = numberOfReplicas;
        this.fields.putAll(fields);
        this.tags.putAll(tags);
        this.tagsFieldName = tagsFieldName;
        load();
    }

    public void load() throws StorageLoaderException {
        //  Check if index exist, otherwise create it
        if (!isIndexExist()) {
            Main.debug("Index '" + indexName + "' not found, creating...");
            createIndex();
        }
        Main.debug("Index '" + indexName + "' loaded !");
    }

    public boolean isIndexExist() throws StorageLoaderException {
        try {
            Main.debug("Check if index '" + indexName + "' is present...");
            return client.indices().exists(e -> e.index(indexName)).value();
        } catch (ElasticsearchException | IOException exception) {
            Main.warning("ElasticSearch client error.");
            Main.stack(exception.getStackTrace());
            throw new StorageLoaderException("Index check error: " + exception.getMessage());
        }
    }

    public void createIndex() throws StorageLoaderException {
        try {
            Main.debug("Creating index '" + indexName + "'...");
            client.indices().create(c -> c.index(indexName)
                    .mappings(m -> m.properties(ESUtils.getMapping(fields, tags, tagsFieldName)))
                    .settings(s -> s.numberOfReplicas(numberOfReplicas))
            );
            Main.debug("Index '" + indexName + "' created !");
        } catch (ElasticsearchException | IOException exception) {
            Main.warning("ElasticSearch client error.");
            Main.stack(exception.getStackTrace());
            throw new StorageLoaderException("Index create error: " + exception.getMessage());
        }
    }
}
