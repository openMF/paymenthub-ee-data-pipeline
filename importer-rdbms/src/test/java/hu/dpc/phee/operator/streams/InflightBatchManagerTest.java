package hu.dpc.phee.operator.streams;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import hu.dpc.phee.operator.config.properties.InflightBatchProperties;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/**
 * The batch file name and batch ID maps used to keep an entry for every batch workflow since the
 * importer started. They now drop a workflow after a period without records, or past a size limit.
 */
class InflightBatchManagerTest {

    private final AtomicLong nanos = new AtomicLong();

    private InflightBatchManager manager(Duration expireAfterAccess, long maximumSize) {
        return new InflightBatchManager(new InflightBatchProperties(expireAfterAccess, maximumSize), nanos::get, Runnable::run);
    }

    @Test
    void entriesExpireAfterAPeriodWithoutAccess() {
        InflightBatchManager manager = manager(Duration.ofMinutes(10), 100);
        manager.storeBatchFileName(1L, "batch-1.csv");
        manager.storeBatchId(1L, "batch-1");

        nanos.addAndGet(Duration.ofMinutes(9).toNanos());
        // reading counts as access, so a batch that is still receiving records is kept
        assertEquals("batch-1.csv", manager.getBatchFileName(1L));
        assertEquals("batch-1", manager.getBatchId(1L));

        nanos.addAndGet(Duration.ofMinutes(9).toNanos());
        assertEquals("batch-1.csv", manager.getBatchFileName(1L));

        nanos.addAndGet(Duration.ofMinutes(11).toNanos());
        assertNull(manager.getBatchFileName(1L));
        assertNull(manager.getBatchId(1L));
    }

    @Test
    void entriesAreEvictedPastTheSizeLimit() {
        InflightBatchManager manager = manager(Duration.ofDays(1), 10);
        for (long key = 0; key < 1000; key++) {
            manager.storeBatchId(key, "batch-" + key);
        }

        long remembered = 0;
        for (long key = 0; key < 1000; key++) {
            if (manager.getBatchId(key) != null) {
                remembered++;
            }
        }
        assertEquals(10, remembered);
    }

    @Test
    void bindsTheShippedPropertyNames() {
        InflightBatchProperties properties = new Binder(new MapConfigurationPropertySource(
                Map.of("importer.inflight-batch.expire-after-access", "24h", "importer.inflight-batch.maximum-size", "100000")))
                .bind("importer.inflight-batch", InflightBatchProperties.class).get();

        assertEquals(Duration.ofHours(24), properties.expireAfterAccess());
        assertEquals(100000L, properties.maximumSize());
    }
}
