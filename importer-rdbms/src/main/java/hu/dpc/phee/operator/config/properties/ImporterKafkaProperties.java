package hu.dpc.phee.operator.config.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Which topic the importer reads and how wide the aggregation window is.
 *
 * <p>
 * aggreationWindowSeconds is spelled the way the property is spelled, typo included
 * ({@code importer.kafka.aggreation-window-seconds}). Fixing the spelling would be a rename of a
 * property a deployment may set, so it is left exactly as it is.
 * </p>
 */
@Validated
@ConfigurationProperties(prefix = "importer.kafka")
public record ImporterKafkaProperties(@NotNull String topic, @NotNull Integer aggreationWindowSeconds) {}
