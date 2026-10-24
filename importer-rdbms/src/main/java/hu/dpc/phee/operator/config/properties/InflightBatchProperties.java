package hu.dpc.phee.operator.config.properties;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Bounds on what InflightBatchManager remembers per batch workflow (its batch file name and batch ID).
 *
 * <p>
 * An entry is dropped once its workflow has had no records for {@code expireAfterAccess}, or when more
 * than {@code maximumSize} workflows are remembered. Either must comfortably exceed the longest
 * running batch and the number of batches in flight at once: a batch whose entry is dropped early
 * is no longer matched to its file. Defaults are in application.yml.
 * </p>
 */
@Validated
@ConfigurationProperties(prefix = "importer.inflight-batch")
public record InflightBatchProperties(@NotNull Duration expireAfterAccess, @NotNull @Positive Long maximumSize) {}
