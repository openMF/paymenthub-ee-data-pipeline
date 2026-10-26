package hu.dpc.phee.operator;

import com.sun.net.httpserver.HttpServer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.kafka.streams.KafkaStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.function.Supplier;

/**
 * The endpoint the deployment's liveness and readiness probes call (port 5000, "/"). It answers 200
 * only while Kafka Streams is RUNNING or REBALANCING, and 503 otherwise, so a pod whose stream has
 * died is restarted instead of looking healthy.
 */
@Service
public class Healthcheck {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private final int port;
    private final Supplier<KafkaStreams.State> streamsState;
    private HttpServer server;

    @Autowired
    public Healthcheck(StreamsBuilderFactoryBean streamsBuilderFactoryBean) {
        this(5000, () -> {
            KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
            return kafkaStreams == null ? null : kafkaStreams.state();
        });
    }

    Healthcheck(int port, Supplier<KafkaStreams.State> streamsState) {
        this.port = port;
        this.streamsState = streamsState;
    }

    static boolean isHealthy(KafkaStreams.State state) {
        return state == KafkaStreams.State.RUNNING || state == KafkaStreams.State.REBALANCING;
    }

    @PostConstruct
    public void start() throws IOException {
        logger.info("starting healthcheck service on port {}", port);
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", exchange -> {
            // The context registered on "/" also matches every other path, so check the
            // path here. Javalin, which this replaced, only answered on "/" and gave 404
            // for anything else.
            int status = 404;
            if ("/".equals(exchange.getRequestURI().getPath())) {
                KafkaStreams.State state = streamsState.get();
                status = isHealthy(state) ? 200 : 503;
                if (status != 200) {
                    logger.warn("healthcheck failing, Kafka Streams state is {}", state);
                }
            }
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
    }

    // The server's thread would otherwise keep the JVM alive after the application context has
    // failed or closed, answering probes for an importer that no longer runs.
    @PreDestroy
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    int boundPort() {
        return server.getAddress().getPort();
    }
}
