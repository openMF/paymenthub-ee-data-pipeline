package hu.dpc.phee.operator.config.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The JDBC pieces every tenant datasource is built from. The per-tenant host, schema and credentials
 * do not live here: they come from tenants.connections, which the deployment mounts as a properties
 * file.
 *
 * <p>
 * driverclass_name keeps its underscore because that is the name in application.yml; relaxed binding
 * matches it to driverclassName, and DeploymentEnvironmentBindingTest pins that down.
 * </p>
 */
@Validated
@ConfigurationProperties(prefix = "datasource.common")
public record DatasourceCommonProperties(@NotNull String protocol, @NotNull String subprotocol, @NotNull String driverclassName) {}
