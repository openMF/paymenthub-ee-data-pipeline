package hu.dpc.phee.operator.config.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The bucket batch files are read from. The deployment sets it as APPLICATION_BUCKET-NAME, an
 * environment variable with a dash in the name.
 */
@Validated
@ConfigurationProperties(prefix = "application")
public record ApplicationProperties(@NotNull String bucketName) {}
