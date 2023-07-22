package fr.milekat.societe.storage.adapter.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import fr.milekat.societe.Main;
import fr.milekat.utils.Configs;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class ESConnection {
    private final RestClient restClient;
    private final ElasticsearchTransport transport;

    public ESConnection(@NotNull Configs config) {
        //  Fetch connections vars from config.yml file
        String hostname = config.getString("storage.elasticsearch.hostname");
        int port = config.getInt("storage.elasticsearch.port", 9200);
        String username = config.getString("storage.elasticsearch.username", "null");
        String password = config.getString("storage.elasticsearch.password", "null");
        //  Check hostname/port
        Main.debug("Hostname:" + hostname);
        Main.debug("Port:" + port);
        Main.debug("Username:" + username);
        //  Init the RestClientBuilder
        RestClientBuilder restClientBuilder = RestClient.builder(new HttpHost(hostname, port));
        //  If credentials are set, apply credentials to RestClientBuilder
        if (!username.equals("null") && !password.equals("null")) {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            restClientBuilder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }

        //  Load RestClientTransport
        restClient = restClientBuilder.build();
        //  Build ElasticsearchTransport
        transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
    }

    public ElasticsearchClient getClient() {
        return new ElasticsearchClient(transport);
    }

    public ElasticsearchAsyncClient getAsyncClient() {
        return new ElasticsearchAsyncClient(transport);
    }

    public void close() throws IOException {
        transport.close();
        restClient.close();
    }
}
