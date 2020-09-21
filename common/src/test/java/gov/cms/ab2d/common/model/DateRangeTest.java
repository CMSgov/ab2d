package gov.cms.ab2d.common.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class DateRangeTest {

    @DisplayName("Detects null or end date before start date")
    @Test
    void invalidArguments() {

        assertThrows(NullPointerException.class, () -> new DateRange(null, null));
        assertThrows(IllegalArgumentException.class, () -> new DateRange(LocalDate.now(), LocalDate.now().minusDays(1)));
    }

    @DisplayName("Applies range function to normal cases correctly")
    @Test
    void rangeCheckFunctions() {

        DateRange dateRange = new DateRange(LocalDate.now(), LocalDate.now().plusMonths(1));

        assertTrue(dateRange.inRange(LocalDateTime.now().plusDays(2)));
        assertFalse(dateRange.inRange(LocalDateTime.now().plusMonths(2)));
        assertFalse(dateRange.inRange(LocalDateTime.now().minusDays(1)));
    }

    @DisplayName("Make sure date range is exclusive at edge")
    @Test
    void edgeCases() {

        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().plusMonths(1);
        DateRange dateRange = new DateRange(start, end);

        // The start date boundary is defined correctly. Inclusive of the start day.
        LocalDateTime startTime = LocalDateTime.of(start, LocalTime.of(0, 0, 0));
        assertTrue(dateRange.inRange(startTime));
        assertFalse(dateRange.inRange(startTime.minusSeconds(1)));

        // The end date is exclusive
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.of(0, 0, 0));
        assertFalse(dateRange.inRange(endTime));
        assertTrue(dateRange.inRange(endTime.minusSeconds(1)));
    }
}
