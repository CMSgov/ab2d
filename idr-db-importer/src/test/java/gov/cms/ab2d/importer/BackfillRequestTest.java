package gov.cms.ab2d.importer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BackfillRequestTest {

    private static final YearMonth OCTOBER_2026 = YearMonth.of(2026, 10);

    @Test
    void parsesMonthAndNormalizesContracts() {
        BackfillRequest request = BackfillRequest.parse(" 2026-06 ", "s4802, S5601,,S4802 ", OCTOBER_2026);

        assertEquals(YearMonth.of(2026, 6), request.month());
        assertEquals(List.of("S4802", "S5601"), request.contracts());
        assertEquals("coverage_v3_backfill_202606.csv", request.s3Key());
    }

    @Test
    void acceptsTheNewestMonthBelowTheArchiveCutoff() {
        // Cutoff in October is August, so July is the newest month the archive could have lost.
        BackfillRequest request = BackfillRequest.parse("2026-07", "S4802", OCTOBER_2026);

        assertEquals(YearMonth.of(2026, 7), request.month());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-08", "2026-09", "2026-10", "2026-11"})
    void rejectsMonthsStillInTheRollingImportWindow(String month) {
        assertThrows(IllegalArgumentException.class, () -> BackfillRequest.parse(month, "S4802", OCTOBER_2026));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-6", "202606", "June 2026", ""})
    void rejectsMalformedMonth(String month) {
        assertThrows(IllegalArgumentException.class, () -> BackfillRequest.parse(month, "S4802", OCTOBER_2026));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " , ", "S48021", "S480", "4802", "S4802;DROP TABLE x"})
    void rejectsMissingOrInvalidContracts(String contracts) {
        assertThrows(IllegalArgumentException.class, () -> BackfillRequest.parse("2026-06", contracts, OCTOBER_2026));
    }

    @Test
    void rejectsNullContracts() {
        assertThrows(IllegalArgumentException.class, () -> BackfillRequest.parse("2026-06", null, OCTOBER_2026));
    }
}
