package org.mifos.ops.zeebe;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.mifos.ops.zeebe.config.properties.ElasticsearchProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@ConfigurationPropertiesScan("org.mifos.ops.zeebe.config.properties")
@Component
public class ZeebeOpsApplication {

    private static final Logger log = LoggerFactory.getLogger(ZeebeOpsApplication.class);

    private final ElasticsearchProperties elasticsearch;

    public ZeebeOpsApplication(ElasticsearchProperties elasticsearch) {
        this.elasticsearch = elasticsearch;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return objectMapper
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Bean
    public RestHighLevelClient client() {

        RestClientBuilder builder;
        SSLContext sslContext = null;
        if (elasticsearch.security().enabled()) {
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(elasticsearch.username(), elasticsearch.password()));
            if (elasticsearch.sslVerification()) {
                SSLContextBuilder sslBuilder;
                try {
                    sslBuilder = SSLContexts.custom().loadTrustMaterial(null, (x509Certificates, s) -> true);
                    sslContext = sslBuilder.build();
                } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
                    log.error("Error building SSL context", e);
                }
                HttpHost httpHost = urlToHttpHost(elasticsearch.url());
                SSLContext finalSslContext = sslContext;
                builder = RestClient.builder(httpHost)
                        .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                                .setSSLContext(finalSslContext)
                                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                                .setDefaultCredentialsProvider(credentialsProvider));
            } else {
                HttpHost httpHost = urlToHttpHost(elasticsearch.url());
                builder = RestClient.builder(httpHost)
                        .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                                .setDefaultCredentialsProvider(credentialsProvider));
            }
        } else {
            HttpHost httpHost = urlToHttpHost(elasticsearch.url());
            builder =
                    RestClient.builder(httpHost).setHttpClientConfigCallback(this::setHttpClientConfigCallback);
        }
        return new RestHighLevelClient(builder);
    }

    private HttpAsyncClientBuilder setHttpClientConfigCallback(HttpAsyncClientBuilder builder) {
        builder.setDefaultIOReactorConfig(IOReactorConfig.custom().setIoThreadCount(1).build());
        return builder;
    }

    private static HttpHost urlToHttpHost(String url) {
        URI uri = null;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            log.error("Error parsing URL: {}", url, e);
        }

        return new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
    }

    // The ElasticsearchOperations/ElasticsearchRestTemplate bean was removed:
    // no code consumed it, and the class no longer exists in Spring Data
    // Elasticsearch 5 (Spring Boot 3). ES access goes through the
    // RestHighLevelClient bean above.

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean
    public Logger logger() {
        return LoggerFactory.getLogger(ZeebeOpsApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(ZeebeOpsApplication.class, args);
    }

}
