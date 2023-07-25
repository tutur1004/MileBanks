package fr.milekat.banks.storage.adapter.elasticsearch;

import fr.milekat.banks.Main;
import fr.milekat.utils.Configs;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.jetbrains.annotations.NotNull;

public class ESConnector {
    private final String hostname;
    private final int port;
    private final String username;
    private final String password;

    public ESConnector(@NotNull Configs config) {
        //  Fetch connections vars from config.yml file
        hostname = config.getString("storage.elasticsearch.hostname");
        port = config.getInt("storage.elasticsearch.port", 9200);
        username = config.getString("storage.elasticsearch.username", "null");
        password = config.getString("storage.elasticsearch.password", "null");
        //  Debug hostname/port
        Main.debug("Hostname:" + hostname);
        Main.debug("Port:" + port);
        Main.debug("Username:" + username);
    }

    private @NotNull RestClient getRestClient() {
        //  Init the RestClientBuilder
        RestClientBuilder restClientBuilder = RestClient.builder(new HttpHost(hostname, port));
        //  If credentials are set, apply credentials to RestClientBuilder
        if (!username.equals("null") && !password.equals("null")) {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            restClientBuilder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }
        return restClientBuilder.build();
    }

    public ESConnection getConnection() {
        return new ESConnection(getRestClient());
    }
}
