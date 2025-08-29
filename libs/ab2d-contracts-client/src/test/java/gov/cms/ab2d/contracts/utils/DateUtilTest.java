package gov.cms.ab2d.contracts.utils;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Date;
import java.util.TimeZone;


import static gov.cms.ab2d.contracts.utils.DateUtil.AB2D_ZONE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DateUtilTest {

    @Test
    void testFormatDateAsDateTimeAsUTC() throws ParseException {
        LocalDateTime localDateTime = LocalDateTime.of(2019, 10, 21, 0, 0);

        String formattedDate = DateUtil.formatLocalDateTimeAsUTC(localDateTime);

        assertEquals("Mon, 21 Oct 2019 00:00:00 GMT", formattedDate);
    }

    @Test
    void testConvertLocalDateTimeToDate() throws ParseException {
        LocalDateTime localDateTime = LocalDateTime.of(2019, 10, 21, 0, 0);
        Date date = DateUtil.convertLocalDateTimeToDate(localDateTime);

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date dateFromFormatParse = format.parse("2019-10-21");

        assertEquals(dateFromFormatParse, date);
    }

    @Test
    void testGetESTOffset() {
        String expectedOffset = TimeZone.getTimeZone(AB2D_ZONE).inDaylightTime(new Date()) ? "-0400" : "-0500";
        String offset = DateUtil.getESTOffset();
        assertEquals(expectedOffset, offset);
    }
}
