package hu.dpc.phee.operator.config.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
 * exactly as the CR writes them, including the ones with a dash in them. The region comes from a
 * secret there; it is set empty here to show that an empty string still binds. If a rename ever
 * creeps in, the build says so instead of a deployment going quiet.
 */
class DeploymentEnvironmentBindingTest {

    private static Binder deploymentEnvironment() {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("APPLICATION_BUCKET-NAME", "paymenthub-ee");
        variables.put("CLOUD_AWS_S3BASEURL", "http://minio:9000");
        variables.put("CLOUD_AWS_REGION_STATIC", "");

        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, variables));
        ConfigurationPropertySources.attach(environment);
        return Binder.get(environment);
    }

    private static Binder propertiesOf(String... pairs) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            properties.put(pairs[i], pairs[i + 1]);
        }
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", properties));
        ConfigurationPropertySources.attach(environment);
        return Binder.get(environment);
    }

    @Test
    void bindsTheBucketNameFromAnEnvironmentVariableWithADash() {
        assertEquals("paymenthub-ee", deploymentEnvironment().bind("application", ApplicationProperties.class).get().bucketName());
    }

    @Test
    void bindsTheAwsValuesIncludingAnEmptyRegion() {
        CloudProperties cloud = deploymentEnvironment().bind("cloud", CloudProperties.class).get();

        // CLOUD_AWS_S3BASEURL matches the renamed s3-base-url spelling as well as the old one
        assertEquals("http://minio:9000", cloud.aws().s3BaseUrl());
        // "static" is a Java keyword, so the component is staticRegion with @Name("static")
        assertEquals("", cloud.aws().region().staticRegion());
    }

    @Test
    void bindsTheApplicationYmlSpellingOfTheS3Url() {
        CloudProperties cloud = propertiesOf("cloud.aws.s3-base-url", "https://s3.ap-south-1.amazonaws.com")
                .bind("cloud", CloudProperties.class).get();

        assertEquals("https://s3.ap-south-1.amazonaws.com", cloud.aws().s3BaseUrl());
    }

    @Test
    void keepsTheUnderscoreInDriverclassName() {
        DatasourceCommonProperties datasource = propertiesOf("datasource.common.driverclass_name", "com.mysql.cj.jdbc.Driver")
                .bind("datasource.common", DatasourceCommonProperties.class).get();

        assertEquals("com.mysql.cj.jdbc.Driver", datasource.driverclassName());
    }

    @Test
    void keepsTheMisspelledAggregationWindowName() {
        // importer.kafka.aggreation-window-seconds, typo and all
        ImporterKafkaProperties kafka = propertiesOf("importer.kafka.topic", "zeebe-export",
                "importer.kafka.aggreation-window-seconds", "2").bind("importer.kafka", ImporterKafkaProperties.class).get();

        assertEquals("zeebe-export", kafka.topic());
        assertEquals(2, kafka.aggreationWindowSeconds());
    }
}
