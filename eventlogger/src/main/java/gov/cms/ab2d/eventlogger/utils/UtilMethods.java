package gov.cms.ab2d.eventlogger.utils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class UtilMethods {

    private UtilMethods() { }

    public static LocalDateTime convertToUtc(OffsetDateTime dt) {
        if (dt != null) {
            // Move it to UTC since that is what it's stored as
            return dt.atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } else {
            return null;
        }
    }
}
