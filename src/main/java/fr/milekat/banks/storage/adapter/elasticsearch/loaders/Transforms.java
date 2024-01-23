package fr.milekat.banks.storage.adapter.elasticsearch.loaders;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch.transform.GetTransformStatsResponse;
import co.elastic.clients.elasticsearch.transform.PivotGroupBy;
import fr.milekat.banks.Main;
import fr.milekat.banks.storage.exceptions.StorageExecuteException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Transforms {
    public Transforms(@NotNull ElasticsearchClient client, @NotNull String indexTransactions,
                      @NotNull String indexAccounts, @NotNull String tagName, @NotNull Class<?> tagType) {
        String transformId = (indexTransactions + "-" + tagName).toLowerCase(Locale.ROOT);
        // Check if transform is present if not create it
        if (!isTransformExist(client, transformId)) {
            try {
                createTransform(client, transformId, indexTransactions, indexAccounts, tagName, tagType);
            } catch (StorageExecuteException exception) {
                Main.debug("Transform create error: " + exception.getMessage());
            }
        }
        // Check if transform is started if not start it
        if (!isTransformStarted(client, transformId)) {
            try {
                startTransform(client, transformId);
            } catch (StorageExecuteException exception) {
                Main.debug("Transform start error: " + exception.getMessage());
            }
        }
    }

    /**
     * Check if transform is present on Elasticsearch server
     * @param client Elasticsearch client
     * @param transformId Transform id
     * @return true if transform is present
     */
    private boolean isTransformExist(@NotNull ElasticsearchClient client, @NotNull String transformId) {
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
     * @param client Elasticsearch client
     * @param transformId Transform id
     * @return true if transform is started
     */
    private boolean isTransformStarted(@NotNull ElasticsearchClient client, @NotNull String transformId) {
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
     * @param client Elasticsearch client
     * @param transformId Transform id
     * @param indexTransactions Index name of transactions
     * @param indexAccounts Index name of accounts
     * @param tagName Tag name
     * @param tagType Tag type
     * @throws StorageExecuteException if transform create error
     */
    private void createTransform(@NotNull ElasticsearchClient client, @NotNull String transformId,
                                 @NotNull String indexTransactions, @NotNull String indexAccounts,
                                 @NotNull String tagName, @NotNull Class<?> tagType) throws StorageExecuteException {
        try {
            Main.debug("Transform '" + transformId + "' creating...");
            client.transform()
                    .putTransform(t -> t
                            .source(s -> s.index(indexTransactions))
                            .pivot(p -> p
                                    .groupBy(getMappingPivotGroup(tagName , tagType))
                                    .aggregations("amount", new Aggregation.Builder()
                                            .sum(s -> s.field("operation"))
                                            .build()
                                    )
                            )
                            .transformId(transformId)
                            .description("Accounts aggregation based on '" + indexTransactions +
                                    " with tag '" + tagName + "'")
                            .dest(d -> d.index(indexAccounts))
                            .sync(s -> s.time(st -> st.field("@timestamp").delay(d -> d.time("3s"))))
                            .frequency(f -> f.time("2s"))
                    );
            Main.debug("Transform '" + transformId + "' created !");
        } catch (ElasticsearchException | IOException exception) {
            throw new StorageExecuteException(exception, "Transform create error: " + exception.getMessage());
        }
    }

    /**
     * Method to format tag to PivotGroupBy
     * @param tagName name of tag
     * @param tagType type of tag
     * @return Formatted PivotGroupBy map
     */
    private @NotNull Map<String, PivotGroupBy> getMappingPivotGroup(@NotNull String tagName,
                                                                    @NotNull Class<?> tagType) {
        Map<String, PivotGroupBy> pivotGroups = new HashMap<>();
        String tagPath = "tags." + tagName;
        if (tagType.equals(String.class)) {
            pivotGroups.put(tagName, new PivotGroupBy.Builder().terms(t -> t.field(tagPath)).build());
        } else if (tagType.equals(Integer.class)) {
            pivotGroups.put(tagName, new PivotGroupBy.Builder().terms(t -> t.field(tagPath)).build());
        } else if (tagType.equals(Float.class)) {
            pivotGroups.put(tagName, new PivotGroupBy.Builder().terms(t -> t.field(tagPath)).build());
        } else if (tagType.equals(Double.class)) {
            pivotGroups.put(tagName, new PivotGroupBy.Builder().terms(t -> t.field(tagPath)).build());
        } else if (tagType.equals(Boolean.class)) {
            pivotGroups.put(tagName, new PivotGroupBy.Builder().terms(t -> t.field(tagPath)).build());
        }
        return pivotGroups;
    }

    /**
     * Start transform on Elasticsearch server
     * @param client Elasticsearch client
     * @param transformId Transform id
     * @throws StorageExecuteException if transform start error
     */
    private void startTransform(@NotNull ElasticsearchClient client, @NotNull String transformId)
            throws StorageExecuteException {
        try {
            Main.debug("Transform '" + transformId + "' is not started, starting...");
            client.transform().startTransform(t -> t.transformId(transformId));
            Main.debug("Transform '" + transformId + "' started !");
        } catch (ElasticsearchException |IOException exception) {
            throw new StorageExecuteException(exception, "Transform start error: " + exception.getMessage());
        }
    }
}
