package gov.cms.ab2d.filter;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Period;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FilterOutByDate {
    public static class DateRange {
        LocalDate start;
        LocalDate end;
        public DateRange(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }
        public boolean inRange(LocalDate d) {
            if (start == null && end != null) {
                return d.isBefore(end);
            }
            if (start != null && end == null) {
                return d.isAfter(start);
            }
            return (d.isAfter(start) && d.isBefore(end));
        }
    }

    public static List<ExplanationOfBenefit> filterByDate(List<ExplanationOfBenefit> benes,
                                                          LocalDate attestationDate,
                                                          List<DateRange> dateRanges) {
        if (benes == null || benes.isEmpty()) {
            return new ArrayList<>();
        }
        List<ExplanationOfBenefit> validBenes = new ArrayList<>();

        for (ExplanationOfBenefit b : benes) {
            boolean inRange = false;
            for (DateRange r : dateRanges) {
                if (withinDateRange(b, r)) {
                    inRange = true;
                    break;
                }
            }
            if (inRange) {
                validBenes.add(b);
            }
        }
        return validBenes;
    }

    public static boolean withinDateRange(ExplanationOfBenefit ben, DateRange range) {
        if (ben == null || ben.getBillablePeriod() == null) {
            return false;
        }
        Period p = ben.getBillablePeriod();
        Date start = ben.getBillablePeriod().getStart();
        Date end = ben.getBillablePeriod().getEnd();
        if (start == null || end == null) {
            return false;
        }
        //return (range.inRange(start) || range.inRange(end));
        return false;
    }
}
