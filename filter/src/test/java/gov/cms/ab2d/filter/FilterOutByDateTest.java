package gov.cms.ab2d.filter;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Resource;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FilterOutByDateTest {
    private SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

    @Test
    void filterByDate() throws ParseException {
        List<FilterOutByDate.DateRange> ranges = List.of(
                FilterOutByDate.getDateRange(10, 2020),
                FilterOutByDate.getDateRange(8, 2020),
                FilterOutByDate.getDateRange(5, 2020, 6, 2020),
                FilterOutByDate.getDateRange(10, 18, 1, 19),
                FilterOutByDate.getDateRange(9, 15),
                FilterOutByDate.getDateRange(10, 12, 5, 15));
        List<ExplanationOfBenefit> list = List.of(
                createEOB("10/01/2020", "10/02/2020"), // In
                createEOB("08/05/2020", "08/06/2020"), // In
                createEOB("11/07/2020", "11/07/2020"), // Out
                createEOB("10/07/2020", "11/07/2020"), // In
                createEOB("10/01/2018", "10/01/2018"), // In
                createEOB("10/01/2000", "10/03/2000"), // Out
                createEOB("10/01/2013", "10/03/2013"), // In
                createEOB("10/31/2020", "11/02/2020")  // In
        );

        assertEquals(6, FilterOutByDate.filterByDate(list, sdf.parse("12/01/2000"), ranges).size());
        assertEquals(5, FilterOutByDate.filterByDate(list, sdf.parse("10/01/2018"), ranges).size());
        assertEquals(0, FilterOutByDate.filterByDate(list, sdf.parse("12/01/2021"), ranges).size());
    }

    @Test
    void testAfterAttestation() throws ParseException {
        ExplanationOfBenefit b = createEOB("10/01/2020", "10/03/2020");
        assertTrue(FilterOutByDate.afterAttestation(sdf.parse("10/01/2020"), b));
        assertTrue(FilterOutByDate.afterAttestation(sdf.parse("10/03/2020"), b));
        assertTrue(FilterOutByDate.afterAttestation(sdf.parse("10/03/2002"), b));
        assertFalse(FilterOutByDate.afterAttestation(sdf.parse("10/04/2020"), b));
    }

    @Test
    void withinDateRange() throws ParseException {
        ExplanationOfBenefit b = createEOB("10/01/2020", "10/03/2020");
        assertTrue(FilterOutByDate.withinDateRange(b, getDateRange("10/02/2020", "10/05/2020")));
        assertTrue(FilterOutByDate.withinDateRange(b, getDateRange("10/01/2020", "10/05/2020")));
        assertTrue(FilterOutByDate.withinDateRange(b, getDateRange("10/03/2020", "10/05/2020")));
        assertTrue(FilterOutByDate.withinDateRange(b, getDateRange("10/01/2020", "10/03/2020")));
        assertTrue(FilterOutByDate.withinDateRange(b, getDateRange("10/02/2020", "10/03/2020")));
        assertTrue(FilterOutByDate.withinDateRange(b, getDateRange("09/02/2020", "10/01/2020")));
        assertTrue(FilterOutByDate.withinDateRange(b, getDateRange("09/02/2020", "10/03/2020")));
        assertFalse(FilterOutByDate.withinDateRange(b, getDateRange("09/02/2020", "09/30/2020")));
        assertFalse(FilterOutByDate.withinDateRange(b, getDateRange("10/04/2020", "10/04/2020")));
        assertFalse(FilterOutByDate.withinDateRange(b, getDateRange("10/04/2020", "10/08/2020")));
    }

    @Test
    void testWeirdDate() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        Date d = sdf.parse("10/01/2020 23:59:59");
        ExplanationOfBenefit b = new ExplanationOfBenefit();
        Period p = new Period();
        p.setStart(d);
        p.setEnd(d);
        b.setBillablePeriod(p);
        assertTrue(FilterOutByDate.withinDateRange(b, getDateRange("09/01/2020", "10/01/2020")));
    }

    @Test
    void testGetStartOfMonth() throws ParseException {
        Date realDate = sdf.parse("10/01/1999");
        Date d = FilterOutByDate.getStartOfMonth(10, 1999);
        assertEquals(sdf.format(d.getTime()), sdf.format(realDate.getTime()));
        d = FilterOutByDate.getStartOfMonth(10, 99);
        assertEquals(sdf.format(d.getTime()), sdf.format(realDate.getTime()));

        realDate = sdf.parse("10/01/2010");
        d = FilterOutByDate.getStartOfMonth(10, 10);
        assertEquals(sdf.format(d.getTime()), sdf.format(realDate.getTime()));
    }

    @Test
    void testGetEndOfMonth() throws ParseException {
        Date realDate = sdf.parse("10/31/1999");
        Date d = FilterOutByDate.getEndOfMonth(10, 1999);
        assertEquals(sdf.format(d.getTime()), sdf.format(realDate.getTime()));
        d = FilterOutByDate.getEndOfMonth(10, 99);
        assertEquals(sdf.format(d.getTime()), sdf.format(realDate.getTime()));

        realDate = sdf.parse("02/29/2020");
        d = FilterOutByDate.getEndOfMonth(2, 2020);
        assertEquals(sdf.format(d.getTime()), sdf.format(realDate.getTime()));
    }

    @Test
    void testGetDateRange() throws ParseException {
        Date realStart = sdf.parse("02/01/2000");
        Date realEnd = sdf.parse("02/29/2000");
        FilterOutByDate.DateRange range = FilterOutByDate.getDateRange(2, 2000);
        assertEquals(sdf.format(range.getStart().getTime()), sdf.format(realStart.getTime()));
        assertEquals(sdf.format(range.getEnd().getTime()), sdf.format(realEnd.getTime()));

        realStart = sdf.parse("02/01/2002");
        realEnd = sdf.parse("02/28/2002");
        range = FilterOutByDate.getDateRange(2, 2002);
        assertEquals(sdf.format(range.getStart().getTime()), sdf.format(realStart.getTime()));
        assertEquals(sdf.format(range.getEnd().getTime()), sdf.format(realEnd.getTime()));

        realStart = sdf.parse("02/01/2002");
        realEnd = sdf.parse("04/30/2002");
        range = FilterOutByDate.getDateRange(2, 2002, 4, 2002);
        assertEquals(sdf.format(range.getStart().getTime()), sdf.format(realStart.getTime()));
        assertEquals(sdf.format(range.getEnd().getTime()), sdf.format(realEnd.getTime()));
    }

    @Test
    void testGetDateRangesFromList() throws ParseException {
        List<Integer> months = List.of(1);
        List<FilterOutByDate.DateRange> ranges = FilterOutByDate.getDateRanges(months, 2020);
        assertEquals(1, ranges.size());
        assertEquals("01/01/2020", sdf.format(ranges.get(0).getStart()));
        assertEquals("01/31/2020", sdf.format(ranges.get(0).getEnd()));

        months = List.of(1, 2);
        ranges = FilterOutByDate.getDateRanges(months, 2020);
        assertEquals(1, ranges.size());
        assertEquals("01/01/2020", sdf.format(ranges.get(0).getStart()));
        assertEquals("02/29/2020", sdf.format(ranges.get(0).getEnd()));

        months = List.of(11, 12);
        ranges = FilterOutByDate.getDateRanges(months, 2020);
        assertEquals(1, ranges.size());
        assertEquals("11/01/2020", sdf.format(ranges.get(0).getStart()));
        assertEquals("12/31/2020", sdf.format(ranges.get(0).getEnd()));

        months = List.of();
        ranges = FilterOutByDate.getDateRanges(months, 2020);
        assertEquals(0, ranges.size());

        months = List.of(5, 7, 8);
        ranges = FilterOutByDate.getDateRanges(months, 2020);
        assertEquals(2, ranges.size());
        assertEquals("05/01/2020", sdf.format(ranges.get(0).getStart()));
        assertEquals("05/31/2020", sdf.format(ranges.get(0).getEnd()));
        assertEquals("07/01/2020", sdf.format(ranges.get(1).getStart()));
        assertEquals("08/31/2020", sdf.format(ranges.get(1).getEnd()));

        months = List.of(1, 2, 3, 4, 5, 9);
        ranges = FilterOutByDate.getDateRanges(months, 2020);
        assertEquals(2, ranges.size());
        assertEquals("01/01/2020", sdf.format(ranges.get(0).getStart()));
        assertEquals("05/31/2020", sdf.format(ranges.get(0).getEnd()));
        assertEquals("09/01/2020", sdf.format(ranges.get(1).getStart()));
        assertEquals("09/30/2020", sdf.format(ranges.get(1).getEnd()));

        months = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        ranges = FilterOutByDate.getDateRanges(months, 2020);
        assertEquals(1, ranges.size());
        assertEquals("01/01/2020", sdf.format(ranges.get(0).getStart()));
        assertEquals("12/31/2020", sdf.format(ranges.get(0).getEnd()));
    }

    private ExplanationOfBenefit createEOB(String startDate, String endDate) throws ParseException {
        ExplanationOfBenefit b = new ExplanationOfBenefit();
        Period p = new Period();
        p.setStart(sdf.parse(startDate));
        p.setEnd(sdf.parse(endDate));
        b.setBillablePeriod(p);
        return b;
    }

    private FilterOutByDate.DateRange getDateRange(String d1, String d2) throws ParseException {
        return new FilterOutByDate.DateRange(sdf.parse(d1), sdf.parse(d2));
    }
}