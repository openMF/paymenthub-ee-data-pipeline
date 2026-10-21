package hu.dpc.phee.operator.streams;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hu.dpc.phee.operator.entity.batch.Batch;
import org.junit.jupiter.api.Test;

/**
 * PHEE-432: Zeebe serialises numbers as decimals ("26.0"); setting them on the batches' Long amount fields failed
 * with a type mismatch, so batch amounts were never persisted by the transformers.
 */
class RecordParserSetFieldTest {

    @Test
    void wholeDecimalGoesIntoLongField() {
        Batch batch = new Batch();

        RecordParser.setField(batch, "totalAmount", "26.0");
        RecordParser.setField(batch, "ongoingAmount", "0.0");
        RecordParser.setField(batch, "completedAmount", "26");

        assertEquals(26L, batch.getTotalAmount());
        assertEquals(0L, batch.getOngoingAmount());
        assertEquals(26L, batch.getCompletedAmount());
    }

    @Test
    void realFractionIsRejectedNotRounded() {
        Batch batch = new Batch();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> RecordParser.setField(batch, "failedAmount", "10.5"));
        assertEquals("field failedAmount holds a whole number, cannot store 10.5", e.getMessage());
    }

    @Test
    void nonNumericFieldsAreUntouched() {
        Batch batch = new Batch();

        RecordParser.setField(batch, "note", "1.5 of 2");

        assertEquals("1.5 of 2", batch.getNote());
    }
}
