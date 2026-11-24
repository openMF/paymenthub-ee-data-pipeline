package hu.dpc.phee.operator.streams;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jayway.jsonpath.DocumentContext;
import hu.dpc.phee.operator.config.TransferTransformerConfig;
import hu.dpc.phee.operator.config.properties.InflightBatchProperties;
import hu.dpc.phee.operator.entity.batch.Batch;
import hu.dpc.phee.operator.entity.batch.BatchRepository;
import hu.dpc.phee.operator.importer.JsonPathReader;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * PHEE-480: a batch's started and completed times come from its flow's process instance, not from
 * every element of the flow. Before, each service task, gateway or timer moved the start time
 * forward and set a completed time, so a running batch showed one.
 */
class RecordParserBatchTimesTest {

    private static final String BPMN = "bulk_processor";
    private static final long INSTANCE = 2251799815431558L;

    private final Batch batch = new Batch(INSTANCE);
    private RecordParser parser;

    @BeforeEach
    void setUp() throws Exception {
        parser = new RecordParser();

        TransferTransformerConfig.Flow flow = new TransferTransformerConfig.Flow();
        flow.setName(BPMN);
        flow.setType("BATCH");
        flow.setTransformers(List.of());
        TransferTransformerConfig config = new TransferTransformerConfig();
        config.setFlows(List.of(flow));
        parser.transferTransformerConfig = config;

        parser.batchRepository = (BatchRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { BatchRepository.class }, (proxy, method, args) -> switch (method.getName()) {
                    case "save" -> args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "BatchRepository fake";
                    default -> throw new UnsupportedOperationException(method.getName());
                });

        InflightBatchManager manager = new InflightBatchManager(new InflightBatchProperties(Duration.ofMinutes(10), 100L)) {

            @Override
            public Batch retrieveOrCreateBatch(String bpmn, DocumentContext record) {
                return batch;
            }

            @Override
            public void checkWorkerIdAndUpdateTransferData(Batch b, Long workflowInstanceKey, Long timestamp) {
                // transfer bookkeeping is not what this test is about
            }
        };
        Field field = RecordParser.class.getDeclaredField("inflightBatchManager");
        field.setAccessible(true);
        field.set(parser, manager);
    }

    private void record(String elementType, String elementId, String intent, long timestamp) {
        DocumentContext document = JsonPathReader.parse("""
                {"recordType":"EVENT","intent":"%s","valueType":"PROCESS_INSTANCE","timestamp":%d,
                 "value":{"processInstanceKey":%d,"bpmnElementType":"%s","elementId":"%s"}}
                """.formatted(intent, timestamp, INSTANCE, elementType, elementId));
        parser.processWorkflowInstance(document, BPMN, INSTANCE, timestamp, elementType, elementId, "BATCH", document);
    }

    @Test
    void timesComeFromTheProcessInstance() {
        record("PROCESS", "bulk_processor-greenbank", "ELEMENT_ACTIVATING", 1_000);
        record("START_EVENT", "StartEvent_1", "ELEMENT_ACTIVATING", 1_010);
        record("START_EVENT", "StartEvent_1", "ELEMENT_COMPLETED", 1_020);
        record("SERVICE_TASK", "Activity_splitting", "ELEMENT_ACTIVATING", 2_000);
        record("SERVICE_TASK", "Activity_splitting", "ELEMENT_COMPLETED", 2_500);
        record("INTERMEDIATE_CATCH_EVENT", "Event_wait", "ELEMENT_ACTIVATING", 3_000);
        record("INTERMEDIATE_CATCH_EVENT", "Event_wait", "ELEMENT_COMPLETED", 3_500);

        assertEquals(1_000, batch.getStartedAt().getTime(), "start is the process start");
        assertNull(batch.getCompletedAt(), "a running batch has no completed time");

        record("END_EVENT", "Event_end", "ELEMENT_ACTIVATING", 4_000);
        record("END_EVENT", "Event_end", "ELEMENT_COMPLETED", 4_010);
        assertNull(batch.getCompletedAt(), "the end event completing is not yet the process completing");

        record("PROCESS", "bulk_processor-greenbank", "ELEMENT_COMPLETED", 4_020);
        assertEquals(1_000, batch.getStartedAt().getTime());
        assertEquals(4_020, batch.getCompletedAt().getTime());
        assertNull(batch.getNote());
    }

    @Test
    void aFailedEndStillGetsTheNoteAndTheProcessEndTime() {
        record("PROCESS", "bulk_processor-greenbank", "ELEMENT_ACTIVATING", 1_000);
        record("END_EVENT", "Event_MergeFailed", "ELEMENT_COMPLETED", 5_000);
        record("PROCESS", "bulk_processor-greenbank", "ELEMENT_COMPLETED", 5_010);

        assertEquals(RecordParser.FAILED_BATCH_NOTE + ": Event_MergeFailed", batch.getNote());
        assertEquals(5_010, batch.getCompletedAt().getTime());
    }

    @Test
    void processStartAndEnd() {
        assertTrue(RecordParser.isProcessStart("PROCESS", "ELEMENT_ACTIVATING"));
        assertFalse(RecordParser.isProcessStart("PROCESS", "ELEMENT_ACTIVATED"));
        assertFalse(RecordParser.isProcessStart("SERVICE_TASK", "ELEMENT_ACTIVATING"));
        assertFalse(RecordParser.isProcessStart(null, "ELEMENT_ACTIVATING"));
        assertTrue(RecordParser.isProcessEnd("PROCESS", "ELEMENT_COMPLETED"));
        assertFalse(RecordParser.isProcessEnd("END_EVENT", "ELEMENT_COMPLETED"));
        assertFalse(RecordParser.isProcessEnd("PROCESS", "ELEMENT_TERMINATED"));
    }
}
