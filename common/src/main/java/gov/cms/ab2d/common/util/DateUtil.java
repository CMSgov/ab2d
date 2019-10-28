package gov.cms.ab2d.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public final class DateUtil {

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
}
