package hu.dpc.phee.operator.streams;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** PHEE-476: a batch flow ending at a failure end event (for example "Merge Failed") is a failed batch. */
class RecordParserBatchEndTest {

    @Test
    void failedBatchEnd() {
        assertTrue(RecordParser.isFailedEnd("END_EVENT", "ELEMENT_COMPLETED", "Event_MergeFailed"));
        assertFalse(RecordParser.isFailedEnd("END_EVENT", "ELEMENT_COMPLETED", "Event_1g7s82g"));
        assertFalse(RecordParser.isFailedEnd("END_EVENT", "ELEMENT_ACTIVATED", "Event_MergeFailed"));
        assertFalse(RecordParser.isFailedEnd("SERVICE_TASK", "ELEMENT_COMPLETED", "mergeFailedCheck"));
        assertFalse(RecordParser.isFailedEnd("END_EVENT", "ELEMENT_COMPLETED", null));
    }
}
