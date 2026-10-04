package hu.dpc.phee.operator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import hu.dpc.phee.operator.config.TransferTransformerConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class})
@Configuration
@EnableConfigurationProperties(value = TransferTransformerConfig.class)
@ConfigurationPropertiesScan("hu.dpc.phee.operator.config.properties")
public class DatabaseImporterApplication {

    static {
        // NOTE zeebe timestamps are also GMT, parsed dates in DB should also use GMT to match this
        System.setProperty("user.timezone", "GMT");
    }

    public static void main(String[] args) {
        SpringApplication.run(DatabaseImporterApplication.class, args);
    }

    @Bean
    public CsvMapper csvMapper() {
        return new CsvMapper();
    }

    /**
     * The JSON mapper the record parsing uses.
     *
     * <p>
     * It has to be declared and marked primary because of the bean above: CsvMapper extends
     * ObjectMapper, so as far as Spring Boot is concerned this context already had an ObjectMapper
     * and its Jackson auto-configuration backed off. The only mapper in the registry wrote CSV,
     * which is why RecordParser was building its own with new ObjectMapper() instead of injecting
     * one - the injection would have handed it the CSV mapper.
     * </p>
     *
     * <p>
     * It is a plain new ObjectMapper() on purpose, which is exactly what the code created per call
     * until now. Configuring it further - for instance so the sub-batch JSON tolerates a field the
     * bulk connector adds - is a separate decision.
     * </p>
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
