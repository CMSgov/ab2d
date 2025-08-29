package gov.cms.ab2d.contracts.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public final class DateUtil {

    public static final int AB2D_EPOCH_YEAR = 2020;

    public static final ZoneId AB2D_ZONE = ZoneId.of("America/New_York");

    public static final ZonedDateTime AB2D_EPOCH = ZonedDateTime.of(2020, 1, 1,
            0, 0, 0, 0, AB2D_ZONE);

    private DateUtil() { }

    // Since this is being sent to the client, return as UTC time
    public static String formatLocalDateTimeAsUTC(LocalDateTime localDateTime) {
        Instant instant = Instant.now();
        ZoneId zone = ZoneId.of("UTC");
        ZoneOffset zoneOffset = zone.getRules().getOffset(instant);
        return DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.ofInstant(localDateTime.toInstant(zoneOffset), ZoneId.of("UTC")));
    }

    public static Date convertLocalDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static String getESTOffset() {
        return String.format("%tz", Instant.now().atZone(AB2D_ZONE));
    }
}
