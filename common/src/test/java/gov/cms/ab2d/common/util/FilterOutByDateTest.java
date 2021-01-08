package gov.cms.ab2d.common.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FilterOutByDateTest {
    private final static SimpleDateFormat SDF = new SimpleDateFormat("MM/dd/yyyy");
    private final static SimpleDateFormat LONGSDF = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss:SSS");

    @BeforeAll
    static void setTimeZone() {
        SDF.setTimeZone(FilterOutByDate.TIMEZONE);
        LONGSDF.setTimeZone(FilterOutByDate.TIMEZONE);
    }

    @Test
    void filterByDate() {

        try {
            List<FilterOutByDate.DateRange> ranges = List.of(
                    FilterOutByDate.getDateRange(10, 2020),
                    FilterOutByDate.getDateRange(8, 2020),
                    FilterOutByDate.getDateRange(5, 2020, 6, 2020),
                    FilterOutByDate.getDateRange(10, 18, 1, 19),
                    FilterOutByDate.getDateRange(9, 15),
                    FilterOutByDate.getDateRange(10, 12, 5, 15));
            List<org.hl7.fhir.dstu3.model.ExplanationOfBenefit> list = List.of(
                    createEOB("10/01/2020", "10/02/2020"), // In
                    createEOB("08/05/2020", "08/06/2020"), // In
                    createEOB("11/07/2020", "11/07/2020"), // Out
                    createEOB("10/07/2020", "11/07/2020"), // In
                    createEOB("10/01/2018", "10/01/2018"), // In
                    createEOB("10/01/2000", "10/03/2000"), // Out
                    createEOB("10/01/2013", "10/03/2013"), // In
                    createEOB("10/31/2020", "11/02/2020")  // In
            );

            assertEquals(6, FilterOutByDate.filterByDate(list, SDF.parse("12/01/2000"), SDF.parse("01/01/2000"), ranges).size());
            assertEquals(5, FilterOutByDate.filterByDate(list, SDF.parse("10/01/2018"), SDF.parse("01/01/2000"), ranges).size());
            assertEquals(0, FilterOutByDate.filterByDate(list, SDF.parse("12/01/2021"), SDF.parse("01/01/2000"), ranges).size());
            assertEquals(4, FilterOutByDate.filterByDate(list, SDF.parse("12/01/2000"), SDF.parse("12/01/2018"), ranges).size());
        } catch (ParseException parseException) {
            fail("could not create eobs", parseException);
        }
    }

    @Test
    void testAfterAttestation() throws ParseException {
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit b = createEOB("10/01/2020", "10/03/2020");
        assertTrue(FilterOutByDate.afterDate(SDF.parse("10/01/2020"), b));
        assertTrue(FilterOutByDate.afterDate(SDF.parse("10/03/2020"), b));
        assertTrue(FilterOutByDate.afterDate(SDF.parse("10/03/2002"), b));
        assertFalse(FilterOutByDate.afterDate(SDF.parse("10/04/2020"), b));
    }

    @Test
    void withinDateRange() throws ParseException {
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit b = createEOB("10/01/2020", "11/01/2020");

        // Any slice of billing period within the interval
        assertTrue(FilterOutByDate.withinDateRange(b, FilterOutByDate.getDateRange(10, 2020)));
        assertTrue(FilterOutByDate.withinDateRange(b, FilterOutByDate.getDateRange(11, 2020)));

        // Slice just outside interval
        assertFalse(FilterOutByDate.withinDateRange(b, FilterOutByDate.getDateRange(9, 2020)));
        assertFalse(FilterOutByDate.withinDateRange(b, FilterOutByDate.getDateRange(12, 2020)));

        // Larger date range
        assertTrue(FilterOutByDate.withinDateRange(b, FilterOutByDate.getDateRange(9, 2020, 10, 2020)));
        assertTrue(FilterOutByDate.withinDateRange(b, FilterOutByDate.getDateRange(10, 2020, 11, 2020)));
        assertTrue(FilterOutByDate.withinDateRange(b, FilterOutByDate.getDateRange(11, 2020, 12, 2020)));
        assertTrue(FilterOutByDate.withinDateRange(b, FilterOutByDate.getDateRange(8, 2020, 12, 2020)));

        assertFalse(FilterOutByDate.withinDateRange(b, FilterOutByDate.getDateRange(6, 2020, 9, 2020)));
    }

    @Test
    void inRange() {

        // Check that inRange is inclusive
        try {
            FilterOutByDate.DateRange range = FilterOutByDate.getDateRange(10, 2020);

            Date beginning = LONGSDF.parse("10/01/2020 00:00:00:000");
            assertTrue(range.inRange(beginning));
            assertFalse(range.inRange(new Date(beginning.getTime() - 1)));

            Date end = LONGSDF.parse("10/31/2020 23:59:59:999");
            assertTrue(range.inRange(end));
            assertFalse(range.inRange(new Date(end.getTime() + 1)));

            Date during = SDF.parse("10/15/2020");
            assertTrue(range.inRange(during));
        } catch (ParseException parseException) {
            fail("could not parse date strings for tests", parseException);
        }

    }

    @Test
    void testGetStartOfMonth() throws ParseException {
        Date realDate = SDF.parse("10/01/1999");
        Date d = FilterOutByDate.getStartOfMonth(10, 1999);
        assertEquals(SDF.format(d.getTime()), SDF.format(realDate.getTime()));
        d = FilterOutByDate.getStartOfMonth(10, 99);
        assertEquals(SDF.format(d.getTime()), SDF.format(realDate.getTime()));

        realDate = SDF.parse("10/01/2010");
        d = FilterOutByDate.getStartOfMonth(10, 10);
        assertEquals(SDF.format(d.getTime()), SDF.format(realDate.getTime()));
    }

    @Test
    void testGetEndOfMonth() throws ParseException {
        Date realDate = SDF.parse("10/31/1999");
        Date d = FilterOutByDate.getEndOfMonth(10, 1999);
        assertEquals(SDF.format(d.getTime()), SDF.format(realDate.getTime()));
        d = FilterOutByDate.getEndOfMonth(10, 99);
        assertEquals(SDF.format(d.getTime()), SDF.format(realDate.getTime()));

        realDate = SDF.parse("02/29/2020");
        d = FilterOutByDate.getEndOfMonth(2, 2020);
        assertEquals(SDF.format(d.getTime()), SDF.format(realDate.getTime()));
    }

    @Test
    void testGetDateRange() throws ParseException {
        Date realStart = SDF.parse("02/01/2000");
        Date realEnd = SDF.parse("02/29/2000");
        FilterOutByDate.DateRange range = FilterOutByDate.getDateRange(2, 2000);
        assertEquals(SDF.format(range.getStart().getTime()), SDF.format(realStart.getTime()));
        assertEquals(SDF.format(range.getEnd().getTime()), SDF.format(realEnd.getTime()));

        realStart = SDF.parse("02/01/2002");
        realEnd = SDF.parse("02/28/2002");
        range = FilterOutByDate.getDateRange(2, 2002);
        assertEquals(SDF.format(range.getStart().getTime()), SDF.format(realStart.getTime()));
        assertEquals(SDF.format(range.getEnd().getTime()), SDF.format(realEnd.getTime()));

        realStart = SDF.parse("02/01/2002");
        realEnd = SDF.parse("04/30/2002");
        range = FilterOutByDate.getDateRange(2, 2002, 4, 2002);
        assertEquals(SDF.format(range.getStart().getTime()), SDF.format(realStart.getTime()));
        assertEquals(SDF.format(range.getEnd().getTime()), SDF.format(realEnd.getTime()));
    }

    @Test
    void testGetDateRangesFromList() {
        List<Integer> months = List.of(1);
        List<FilterOutByDate.DateRange> ranges = FilterOutByDate.getDateRanges(months, 2020);
        assertEquals(1, ranges.size());
        assertEquals("01/01/2020", SDF.format(ranges.get(0).getStart()));
        assertEquals("01/31/2020", SDF.format(ranges.get(0).getEnd()));

        months = List.of(1, 2);
        ranges = FilterOutByDate.getDateRanges(months, 2020);
        assertEquals(1, ranges.size());
        assertEquals("01/01/2020", SDF.format(ranges.get(0).getStart()));
        assertEquals("02/29/2020", SDF.format(ranges.get(0).getEnd()));

        months = List.of(11, 12);
        ranges = FilterOutByDate.getDateRanges(months, 2020);
        assertEquals(1, ranges.size());
        assertEquals("11/01/2020", SDF.format(ranges.get(0).getStart()));
        assertEquals("12/31/2020", SDF.format(ranges.get(0).getEnd()));

        months = List.of();
        ranges = FilterOutByDate.getDateRanges(months, 2020);
        assertEquals(0, ranges.size());

        months = List.of(5, 7, 8);
        ranges = FilterOutByDate.getDateRanges(months, 2020);
        assertEquals(2, ranges.size());
        assertEquals("05/01/2020", SDF.format(ranges.get(0).getStart()));
        assertEquals("05/31/2020", SDF.format(ranges.get(0).getEnd()));
        assertEquals("07/01/2020", SDF.format(ranges.get(1).getStart()));
        assertEquals("08/31/2020", SDF.format(ranges.get(1).getEnd()));

        months = List.of(1, 2, 3, 4, 5, 9);
        ranges = FilterOutByDate.getDateRanges(months, 2020);
        assertEquals(2, ranges.size());
        assertEquals("01/01/2020", SDF.format(ranges.get(0).getStart()));
        assertEquals("05/31/2020", SDF.format(ranges.get(0).getEnd()));
        assertEquals("09/01/2020", SDF.format(ranges.get(1).getStart()));
        assertEquals("09/30/2020", SDF.format(ranges.get(1).getEnd()));

        months = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        ranges = FilterOutByDate.getDateRanges(months, 2020);
        assertEquals(1, ranges.size());
        assertEquals("01/01/2020", SDF.format(ranges.get(0).getStart()));
        assertEquals("12/31/2020", SDF.format(ranges.get(0).getEnd()));
    }

    private org.hl7.fhir.dstu3.model.ExplanationOfBenefit createEOB(String startDate, String endDate) throws ParseException {
        org.hl7.fhir.dstu3.model.ExplanationOfBenefit b = new org.hl7.fhir.dstu3.model.ExplanationOfBenefit();
        org.hl7.fhir.dstu3.model.Period p = new org.hl7.fhir.dstu3.model.Period();
        p.setStart(SDF.parse(startDate));
        p.setEnd(SDF.parse(endDate));
        b.setBillablePeriod(p);
        return b;
    }
}