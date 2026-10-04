package hu.dpc.phee.operator.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;
import org.springframework.validation.annotation.Validated;

/**
 * The object storage batch files are read from.
 *
 * <p>
 * Every value is required, as it was when it was a bare {@code @Value} field. {@code @NotNull} accepts
 * an empty string, as a bare {@code @Value} did, so an empty value from the deployment's secret still
 * starts the pod.
 * </p>
 *
 * <p>
 * cloud.aws.enabled is not here: the {@code @ConditionalOnProperty} on AwsStorageConfig reads it,
 * and for it a missing key means off, not an error.
 * </p>
 *
 * <p>
 * Two names need care. "static" is a Java keyword, so the component is staticRegion and is mapped
 * back with {@link Name}. And the application.yml key s3BaseUrl had to become s3-base-url: a name
 * with a capital letter in the middle is not a valid configuration property name and the binder
 * rejects it outright. The deployment is unaffected, CLOUD_AWS_S3BASEURL matches both spellings.
 * </p>
 */
@Validated
@ConfigurationProperties(prefix = "cloud")
public record CloudProperties(@NotNull @Valid Aws aws) {

    public record Aws(@NotNull @Valid Credentials credentials, @NotNull @Valid Region region, @NotNull String s3BaseUrl) {}

    public record Credentials(@NotNull String accessKey, @NotNull String secretKey) {}

    public record Region(@Name("static") @NotNull String staticRegion) {}
}
