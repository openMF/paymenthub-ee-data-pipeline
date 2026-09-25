package org.mifos.ops.zeebe.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The Zeebe gateway this service talks to. The deployment sets ZEEBE_BROKER_CONTACTPOINT.
 *
 * <p>
 * zeebe.client.evenly-allocated-max-jobs is deliberately not a component here: its value in
 * application.yml is a SpEL expression over the other two, which only @Value evaluates, and nothing
 * in this module reads it.
 * </p>
 */
@Validated
@ConfigurationProperties(prefix = "zeebe")
public record ZeebeProperties(@NotNull @Valid Broker broker, @NotNull @Valid Client client) {

    public record Broker(@NotNull String contactpoint) {}

    public record Client(@NotNull Integer maxExecutionThreads) {}
}
