package fr.milekat.banks.storage.adapter.elasticsearch.loaders;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch.transform.GetTransformStatsResponse;
import co.elastic.clients.elasticsearch.transform.PivotGroupBy;
import co.elastic.clients.elasticsearch.transform.PutTransformRequest;
import fr.milekat.banks.Main;
import fr.milekat.banks.storage.adapter.elasticsearch.ESUtils;
import fr.milekat.banks.storage.adapter.elasticsearch.TransformType;
import fr.milekat.banks.storage.exceptions.StorageLoaderException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

public class Transforms {
    private final ElasticsearchClient client;
    private final TransformType transformType;
    private final String transformId;
    private final String indexSource;
    private final String indexDestination;
    private final Map<String, Class<?>> pivotGroups = new HashMap<>();
    private final List<String> latestFields = new ArrayList<>();
    private final String sortedField;

    @SuppressWarnings("unused")
    public Transforms(@NotNull ElasticsearchClient client,
                      @NotNull String indexSource,
                      @NotNull String indexDestination,
                      @NotNull Map<String, Class<?>> pivotGroups) throws StorageLoaderException {
        this.transformType = TransformType.PIVOT;
        this.client = client;
        this.transformId = (indexDestination).toLowerCase(Locale.ROOT);
        this.indexSource = indexSource;
        this.indexDestination = indexDestination;
        this.pivotGroups.putAll(pivotGroups);
        this.sortedField = null;
        load();
    }

    @SuppressWarnings("unused")
    public Transforms(@NotNull ElasticsearchClient client,
                      @NotNull String indexSource,
                      @NotNull String indexDestination,
                      @NotNull List<String> latestFields,
                      @NotNull String sortedField) throws StorageLoaderException {
        this.transformType = TransformType.LATEST;
        this.client = client;
        this.transformId = (indexSource + "-" + indexDestination).toLowerCase(Locale.ROOT);
        this.indexSource = indexSource;
        this.indexDestination = indexDestination;
        this.latestFields.addAll(latestFields);
        this.sortedField = sortedField;
        load();
    }

    public void load() throws StorageLoaderException {
        Main.debug("Transform '" + transformId + "' loading...");
        // Check if transform is present if not create it
        if (!isTransformExist()) {
            createTransform();
        }
        // Check if transform is started if not start it
        if (!isTransformStarted()) {
            startTransform();
        }
        Main.debug("Transform '" + transformId + "' loaded and started !");
    }

    /**
     * Check if transform is present on Elasticsearch server
     * @return true if transform is present
     */
    private boolean isTransformExist() {
        try {
            Main.debug("Check if transform '" + transformId + "' is present...");
            client.transform().getTransform(t -> t.transformId(transformId));
            Main.debug("Transform '" + transformId + "' is present !");
            return true;
        } catch (ElasticsearchException | IOException exception) {
            Main.debug("Transform '" + transformId + "' is not present !");
            return false;
        }
    }

    /**
     * Check if transform is started
     * @return true if transform is started
     */
    private boolean isTransformStarted() {
        try {
            Main.debug("Check if transform '" + transformId + "' is started...");
            GetTransformStatsResponse getStatsResponse = client.transform()
                    .getTransformStats(t -> t.transformId(transformId));
            if (!getStatsResponse.transforms().get(0).state().equalsIgnoreCase("started")) {
                Main.debug("Transform '" + transformId + "' is not started !");
                return false;
            } else {
                Main.debug("Transform '" + transformId + "' is started !");
                return true;
            }
        } catch (ElasticsearchException |IOException exception) {
            Main.debug("Transform '" + transformId + "' is not started !");
            return false;
        }
    }

    /**
     * Create transform on Elasticsearch server
     * @throws StorageLoaderException if transform create error
     */
    private void createTransform() throws StorageLoaderException {
        Map<String, PivotGroupBy> pivotGroups = new HashMap<>(this.pivotGroups.entrySet()
                .stream()
                .collect(HashMap::new,
                        (m, v) -> m.putAll(ESUtils.getMappingPivotGroup(
                                "tags." + v.getKey(), v.getValue(), v.getKey())),
                        HashMap::putAll
                ));
        String description = "Accounts aggregation based on '" + indexSource + "' with tags: " +
                String.join(", ", pivotGroups.keySet());

        PutTransformRequest.Builder builder = new PutTransformRequest.Builder()
                .source(s -> s.index(indexSource))
                .transformId(transformId)
                .description(description)
                .dest(d -> d.index(indexDestination))
                .sync(s -> s.time(st -> st.field("@timestamp").delay(d -> d.time("3s"))))
                .frequency(f -> f.time("2s"));

        if (transformType.equals(TransformType.PIVOT)) {
            builder.pivot(p -> p
                    .groupBy(pivotGroups)
                    .aggregations("amount", new Aggregation.Builder()
                            .sum(s -> s.field("operation"))
                            .build()
                    )
            );
        } else if (transformType.equals(TransformType.LATEST)) {
            builder.latest(l -> l
                    .uniqueKey(this.latestFields)
                    .sort(this.sortedField)
            );
        }

        try {
            Main.debug("Transform '" + transformId + "' creating...");
            client.transform().putTransform(builder.build());
            Main.debug("Transform '" + transformId + "' created !");
        } catch (ElasticsearchException | IOException exception) {
            throw new StorageLoaderException("Transform create error: " + exception.getMessage());
        }
    }

    /**
     * Start transform on Elasticsearch server
     * @throws StorageLoaderException if transform start error
     */
    private void startTransform() throws StorageLoaderException {
        try {
            Main.debug("Transform '" + transformId + "' is not started, starting...");
            client.transform().startTransform(t -> t.transformId(transformId));
            Main.debug("Transform '" + transformId + "' started !");
        } catch (ElasticsearchException |IOException exception) {
            throw new StorageLoaderException("Transform start error: " + exception.getMessage());
        }
    }
}
