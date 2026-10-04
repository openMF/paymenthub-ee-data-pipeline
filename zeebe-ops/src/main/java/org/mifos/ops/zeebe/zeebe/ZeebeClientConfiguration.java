package org.mifos.ops.zeebe.zeebe;

import io.camunda.zeebe.client.ZeebeClient;
import org.mifos.ops.zeebe.config.properties.ZeebeProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZeebeClientConfiguration {

    private final ZeebeProperties properties;

    public ZeebeClientConfiguration(ZeebeProperties properties) {
        this.properties = properties;
    }

    @Bean
    public ZeebeClient zeebeClient() {
        return ZeebeClient.newClientBuilder()
                .gatewayAddress(properties.broker().contactpoint())
                .usePlaintext()
                .numJobWorkerExecutionThreads(properties.client().maxExecutionThreads())
                .build();
    }
}
