package gov.cms.ab2d.filter;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Period;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class FilterTest {
    @Test
    void testFilter() throws ParseException {
        assertTrue(FilterEob.filter(null, null, null, null, true).isEmpty());
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.US);

        ExplanationOfBenefit eob1 = new ExplanationOfBenefit();
        IBaseResource eob2 = ExplanationOfBenefitTrimmerSTU3.getBenefit(EOBLoadUtilities.getSTU3EOBFromFileInClassPath("eobdata/EOB-for-Part-D-Claims.json"));
        assertTrue(FilterEob.filter(eob1, null, null, null, true).isEmpty());
        assertTrue(FilterEob.filter(eob1, null, null, new Date(), true).isPresent());
        assertTrue(FilterEob.filter(eob1, null, null, new Date(), false).isEmpty());
        assertTrue(FilterEob.filter(eob2, null, null, new Date(), false).isEmpty());

        Period period = new Period();
        period.setStart(sdf.parse("12/01/2019 00:00"));
        period.setEnd(sdf.parse("12/31/2019 00:00"));
        eob1.setBillablePeriod(period);

        Date earliest = sdf.parse("01/01/2020 00:00");

        assertTrue(FilterEob.filter(eob1, null, earliest, new Date(), false).isEmpty());

        earliest = sdf.parse("01/01/2019 00:00");
        assertTrue(FilterEob.filter(eob1, null, earliest, new Date(), false).isEmpty());
        assertTrue(FilterEob.filter(eob1, null, earliest, earliest, false).isEmpty());

        List<FilterOutByDate.DateRange> dateRanges = FilterOutByDate.getDateRanges(List.of(11, 12), 2019);
        assertTrue(FilterEob.filter(eob1, dateRanges, earliest, earliest, false).isPresent());
    }
}
