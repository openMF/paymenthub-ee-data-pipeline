package org.mifos.ops.zeebe.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Where the Elasticsearch the /es routes query lives. Every value is required, as it was when it was
 * a bare {@code @Value} field.
 */
@Validated
@ConfigurationProperties(prefix = "elasticsearch")
public record ElasticsearchProperties(@NotNull String url, @NotNull @Valid Security security, @NotNull Boolean sslVerification,
        @NotNull String username, @NotNull String password) {

    public record Security(@NotNull Boolean enabled) {}
}
