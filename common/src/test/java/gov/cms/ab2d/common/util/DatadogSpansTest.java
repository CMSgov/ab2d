package gov.cms.ab2d.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies the no-agent contract: with no dd-java-agent attached (as in unit tests) there is no
 * active span, so every helper call must be a safe no-op rather than throwing.
 */
class DatadogSpansTest {

    @Test
    void activeSpanIsNullWithoutAgent() {
        assertNull(DatadogSpans.activeSpan());
    }

    @Test
    void taggingIsNoOpWithoutAgent() {
        assertDoesNotThrow(() -> {
            DatadogSpans.setTag("contract", "Z1234");
            DatadogSpans.setTag("count", 5);
            DatadogSpans.setTag("error", true);
            DatadogSpans.setMetric("job.duration", 1000L);
            DatadogSpans.setMetric("eob.response_time", 12.5d);
            DatadogSpans.markError(new RuntimeException("boom"));
            DatadogSpans.markError(null);
        });
    }
}
