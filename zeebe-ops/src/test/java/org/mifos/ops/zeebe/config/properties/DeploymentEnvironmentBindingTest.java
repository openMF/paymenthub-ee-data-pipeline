package org.mifos.ops.zeebe.config.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

/**
 * These bind from the environment of the running gazelle deployment, with the variable names written
 * exactly as the CR writes them. The Elasticsearch username and password come from a secret there;
 * they are set empty here to show that an empty string still binds. ELASTICSEARCH_SSLVERIFICATION is
 * the one to watch: the application.yml key had to be
 * renamed from sslVerification to ssl-verification, and this fixes that the variable the deployment
 * sets still reaches it.
 */
class DeploymentEnvironmentBindingTest {

    private static Binder deploymentEnvironment() {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("ZEEBE_BROKER_CONTACTPOINT", "paymenthub-infra-zeebe-gateway:26500");
        variables.put("ELASTICSEARCH_URL", "http://infra-elasticsearch.infra.svc.cluster.local:9200/");
        variables.put("ELASTICSEARCH_SECURITY_ENABLED", "false");
        variables.put("ELASTICSEARCH_SSLVERIFICATION", "false");
        variables.put("ELASTICSEARCH_USERNAME", "");
        variables.put("ELASTICSEARCH_PASSWORD", "");

        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, variables));
        ConfigurationPropertySources.attach(environment);
        return Binder.get(environment);
    }

    @Test
    void bindsTheElasticsearchSettingsTheDeploymentSets() {
        ElasticsearchProperties elasticsearch = deploymentEnvironment().bind("elasticsearch", ElasticsearchProperties.class).get();

        assertEquals("http://infra-elasticsearch.infra.svc.cluster.local:9200/", elasticsearch.url());
        assertFalse(elasticsearch.security().enabled());
        assertFalse(elasticsearch.sslVerification());
        assertEquals("", elasticsearch.username());
        assertEquals("", elasticsearch.password());
    }

    @Test
    void bindsTheApplicationYmlSpellingOfSslVerification() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("elasticsearch.ssl-verification", "true");
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", properties));
        ConfigurationPropertySources.attach(environment);

        assertTrue(Binder.get(environment).bind("elasticsearch", ElasticsearchProperties.class).get().sslVerification());
    }

    @Test
    void bindsTheZeebeGatewayTheDeploymentSets() {
        ZeebeProperties zeebe = deploymentEnvironment().bind("zeebe", ZeebeProperties.class).get();

        assertEquals("paymenthub-infra-zeebe-gateway:26500", zeebe.broker().contactpoint());
    }
}
