package org.mifos.phee.operator.streams;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.jayway.jsonpath.DocumentContext;
import org.mifos.phee.operator.config.TransferTransformerConfig;
import org.mifos.phee.operator.config.properties.InflightBatchProperties;
import org.mifos.phee.operator.entity.batch.Batch;
import org.mifos.phee.operator.entity.batch.BatchRepository;
import org.mifos.phee.operator.importer.JsonPathReader;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A Zeebe variable set to null is exported as the JSON literal null. The importer stored it as the
 * string "null": a batch submitted without a registering institution had registering_institution_id
 * 'null' rather than NULL.
 */
class RecordParserNullVariableTest {

    private static final String BPMN = "bulk_processor";
    private static final long INSTANCE = 2251799815439260L;

    private final Batch batch = new Batch(INSTANCE);
    private RecordParser parser;

    @BeforeEach
    void setUp() throws Exception {
        parser = new RecordParser();

        TransferTransformerConfig.Transformer transformer = new TransferTransformerConfig.Transformer();
        transformer.setField("registeringInstitutionId");
        transformer.setVariableName("registeringInstituteId");
        TransferTransformerConfig.Flow flow = new TransferTransformerConfig.Flow();
        flow.setName(BPMN);
        flow.setType("BATCH");
        flow.setTransformers(List.of(transformer));
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
        };
        Field field = RecordParser.class.getDeclaredField("inflightBatchManager");
        field.setAccessible(true);
        field.set(parser, manager);
    }

    /** {@code jsonValue} is the variable's value as Zeebe exports it: JSON text. */
    private void variable(String name, String jsonValue) throws Exception {
        String escaped = jsonValue.replace("\\", "\\\\").replace("\"", "\\\"");
        DocumentContext document = JsonPathReader.parse("""
                {"recordType":"EVENT","intent":"CREATED","valueType":"VARIABLE","timestamp":1795431473883,
                 "value":{"processInstanceKey":%d,"name":"%s","value":"%s"}}
                """.formatted(INSTANCE, name, escaped));
        parser.processVariable(document, BPMN, INSTANCE, 1L, 1795431473883L, "BATCH", document);
    }

    @Test
    void aNullVariableIsStoredAsNull() throws Exception {
        variable("registeringInstituteId", "null");
        assertNull(batch.getRegisteringInstitutionId());
    }

    @Test
    void aStringVariableIsStoredWithoutItsQuotes() throws Exception {
        variable("registeringInstituteId", "\"greenbank\"");
        assertEquals("greenbank", batch.getRegisteringInstitutionId());
    }

    @Test
    void theString_null_isKept() throws Exception {
        variable("registeringInstituteId", "\"null\"");
        assertEquals("null", batch.getRegisteringInstitutionId());
    }

    @Test
    void aVariableSetToNullClearsTheField() throws Exception {
        variable("registeringInstituteId", "\"greenbank\"");
        variable("registeringInstituteId", "null");
        assertNull(batch.getRegisteringInstitutionId());
    }

    @Test
    void fieldValue() {
        assertNull(RecordParser.fieldValue("null", "null"));
        assertEquals("null", RecordParser.fieldValue("\"null\"", "null"));
        assertEquals("26.0", RecordParser.fieldValue("26.0", "26.0"));
    }
}
