package hu.dpc.phee.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.streams.KafkaStreams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * The probe endpoint used to answer 200 whatever state the importer was in. It now follows the
 * Kafka Streams state.
 */
class HealthcheckTest {

    private final AtomicReference<KafkaStreams.State> state = new AtomicReference<>();
    private final HttpClient client = HttpClient.newHttpClient();
    private Healthcheck healthcheck;

    @BeforeEach
    void setUp() throws Exception {
        healthcheck = new Healthcheck(0, state::get);
        healthcheck.start();
    }

    @AfterEach
    void tearDown() {
        healthcheck.stop();
    }

    @ParameterizedTest
    @EnumSource(value = KafkaStreams.State.class, names = {"RUNNING", "REBALANCING"})
    void upWhileTheStreamRuns(KafkaStreams.State streamsState) throws Exception {
        state.set(streamsState);
        assertEquals(200, get("/"));
    }

    @ParameterizedTest
    @EnumSource(value = KafkaStreams.State.class, names = {"CREATED", "ERROR", "NOT_RUNNING", "PENDING_SHUTDOWN", "PENDING_ERROR"})
    void downOtherwise(KafkaStreams.State streamsState) throws Exception {
        state.set(streamsState);
        assertEquals(503, get("/"));
    }

    @Test
    void downBeforeTheStreamExists() throws Exception {
        assertEquals(503, get("/"));
    }

    @Test
    void otherPathsAreNotFound() throws Exception {
        state.set(KafkaStreams.State.RUNNING);
        assertEquals(404, get("/other"));
    }

    private int get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + healthcheck.boundPort() + path)).build();
        return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
