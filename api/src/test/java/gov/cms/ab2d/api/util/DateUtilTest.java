package gov.cms.ab2d.api.util;

import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

public class DateUtilTest {

    @Test
    public void testFormatDateAsDateTime() throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        String dateString = format.format( new Date()   );
        Date date = format.parse ("2019-10-21");
        String formattedDate = DateUtil.formatDateAsDateTime(date);
        Assert.assertEquals("Mon, 21 Oct 2019 05:00:00 GMT", formattedDate);
    }

    @Test
    public void testConvertLocalDateTimeToDate() throws ParseException {
        LocalDateTime localDateTime = LocalDateTime.of(2019, 10, 21, 0, 0);
        Date date = DateUtil.convertLocalDateTimeToDate(localDateTime);

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date dateFromFormatParse = format.parse("2019-10-21");

        Assert.assertEquals(dateFromFormatParse, date);
    }
}
