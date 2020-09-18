package gov.cms.ab2d.common.model;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Date range class used to define a from and to date for a subscribers membership.
 * We only deal with days, not hours or minutes. If the date range is 10/01/2020 - 10/02/2020,
 * the range is 10/01/2020 00:00:00 - 10/02/2020 23:59:59. If the start date is after the end date
 * nothing will resolve to being in that range
 */
@Data
public class DateRange {

    private LocalDateTime start;
    private LocalDateTime end;

    /**
     * Populate the date range
     *
     * @param start - the start date of the range
     * @param end   - the end date of the range
     */
    public DateRange(LocalDate start, LocalDate end) {
        this.start = LocalDateTime.of(start, LocalTime.of(0, 0, 0));
        this.end = LocalDateTime.of(end, LocalTime.of(23, 59, 59));
    }

    /**
     * True if a date is in range between the start date and the end date
     *
     * @param d - the date to compare
     * @return true if the date is in range
     */
    public boolean inRange(LocalDateTime d) {
        return start.isBefore(d) && end.isAfter(d);
    }
}
