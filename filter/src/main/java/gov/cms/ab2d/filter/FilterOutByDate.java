package gov.cms.ab2d.filter;

import lombok.Data;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Period;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class to take different subscription ranges and attestation dates and
 * determine if an explanation of benefit object should be filtered out based on
 * those dates. Older Date objects are used instead of more modern dates because the
 * explanation of benefit object billing dates periods are in dates. For consistency
 * and to minimize time zone issues, we kept everything as Date objects.
 */
public final class FilterOutByDate {
    private static final String SHORT = "MM/dd/yyyy";
    private static final String FULL = "MM/dd/yyyy HH:mm:ss";

    /**
     * Date range class used to define a from and to date for a subscribers membership.
     * We only deal with days, not hours or minutes. If the date range is 10/01/2020 - 10/02/2020,
     * the range is 10/01/2020 00:00:00 - 10/02/2020 23:59:59. If the start date is after the end date
     * nothing will resolve to being in that range
     */
    @Data
    public static class DateRange {
        private Date start;
        private Date end;

        /**
         * Populate the date range
         *
         * @param start - the start date of the range
         * @param end   - the end date of the range
         * @throws ParseException if there was an error constructing the Date objects
         */
        public DateRange(Date start, Date end) throws ParseException {
            SimpleDateFormat fullDateFormat = new SimpleDateFormat(FULL);
            SimpleDateFormat shortDateFormat = new SimpleDateFormat(SHORT);
            if (start != null) {
                // we're only dealing with dates, not times, so 0 out time
                this.start = shortDateFormat.parse(shortDateFormat.format(start));
            }
            if (end != null) {
                // we're only dealing with dates, not times, so max out time
                this.end = fullDateFormat.parse(shortDateFormat.format(end) + " 23:59:59");
            }
        }

        /**
         * True if a date is in range between the start date and the end date
         *
         * @param d - the date to compare
         * @return true if the date is in range
         * @throws ParseException if there was an error constructing the Date objects
         */
        public boolean inRange(Date d) throws ParseException {
            // we're only dealing with dates, not times, so 0 out time
            if (start == null && end != null) {
                return d.getTime() <= end.getTime();
            }
            if (start != null && end == null) {
                return d.getTime() >= start.getTime();
            }
            return d.getTime() <= end.getTime() && d.getTime() >= start.getTime();
        }
    }

    /**
     * We should never have to construct this object
     */
    private FilterOutByDate() {
    }

    /**
     * Method to parse a list of months included and return the date ranges implied by the list.
     * For example, a list of (1, 2, 4, 6) in the year 2020 would end up with two time ranges -
     * 1/1/2020 - 2/29/2020 and 4/1/2020 - 6/30/2020
     *
     * @param months - the list of months to include
     * @param year - the year to include
     * @return the list of date ranges
     * @throws ParseException if a date manipulation error occurs
     */
    public static List<DateRange> getDateRanges(List<Integer> months, int year) throws ParseException {
        List<DateRange> ranges = new ArrayList<>();
        if (months == null || months.isEmpty() || year == 0) {
            return ranges;
        }
        List<Integer> monthList = new ArrayList<>(12);
        for (int i = 0; i < 12; i++) {
            monthList.add(0);
        }
        for (int i = 0; i < months.size(); i++) {
            int m = months.get(i);
            monthList.set(m - 1, 1);
        }
        months.forEach(c -> monthList.set(c - 1, 1));
        int startVal = -1;
        int endVal = -1;
        boolean in = false;
        for (int i = 0; i < monthList.size(); i++) {
            if (monthList.get(i) == 1) {
                if (!in) {
                    startVal = i;
                }
                in = true;
            } else {
                if (in) {
                    endVal = i - 1;
                    ranges.add(getDateRange(startVal + 1, year, endVal + 1, year));
                }
               in = false;
            }
        }
        if (in) {
            ranges.add(getDateRange(startVal + 1, year, 12, year));
        }
        return ranges;
    }

    /**
     * Given a month and year, get us the Date that is at the beginning of the month. For example,
     * 10, 2020 = 10/01/2020 00:00:00
     *
     * @param month - month to choose
     * @param year  - year to choose
     * @return the beginning of the month
     */
    static Date getStartOfMonth(int month, int year) {
        Calendar c = Calendar.getInstance();
        c.set(getYearToUse(year), month - 1, 1, 0, 0);
        return c.getTime();
    }

    /**
     * If we are given a 2 digit year, return the 4 digit equivalent
     *
     * @param year - the year, 2 or 4 digits (i.e., 1999 or 99)
     * @return the 4 digit year
     */
    static int getYearToUse(int year) {
        Calendar c = Calendar.getInstance();
        int yearToUse = year;
        if (year < 100) {
            int currentYear = c.get(Calendar.YEAR);
            if (currentYear + 10 < 2000 + year) {
                yearToUse = 1900 + year;
            } else {
                yearToUse = 2000 + year;
            }
        }
        return yearToUse;
    }

    /**
     * Get the date corresponding with the end of the month. For example, 2, 2020 = 02/29/2020 23:59:59
     *
     * @param month - the month to get the end of
     * @param year  - the year
     * @return the end of the month in days, hours, minutes and seconds
     */
    static Date getEndOfMonth(int month, int year) {
        Calendar c = Calendar.getInstance();
        c.set(getYearToUse(year), month - 1, 1, 23, 59);
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        return c.getTime();
    }

    /**
     * Create a date range with just the month and year notation. For example, 10/2020, 11/2020 will
     * end up being 10/01/2020 00:00:00 - 11/30/2020 59:59:59
     *
     * @param startMonth - the start month
     * @param startYear  - the start year
     * @param endMonth   - the end month
     * @param endYear    - the end year
     * @return the date range
     * @throws ParseException if there is an issue parsing the date information
     */
    public static DateRange getDateRange(int startMonth, int startYear, int endMonth, int endYear) throws ParseException {
        return new DateRange(getStartOfMonth(startMonth, startYear), getEndOfMonth(endMonth, endYear));
    }

    /**
     * Create a date range with just the month and year notation. For example, 10/2020 will
     * end up being 10/01/2020 00:00:00 - 10/31/2020 59:59:59
     *
     * @param month - the start and end month
     * @param year - the start and end year
     * @return the date range
     * @throws ParseException if there is an issue parsing the date information
     */
    public static DateRange getDateRange(int month, int year) throws ParseException {
        return new DateRange(getStartOfMonth(month, year), getEndOfMonth(month, year));
    }

    /**
     * This does most of the work of the class. It takes a list of explanation of benefit objects,
     * the attestation date and list of valid date ranges and returns the list of qualifying objects
     *
     * @param benes - the explanation of benefit objects
     * @param attestationDate - the attestation date
     * @param dateRanges - the list of date ranges
     * @return - the list of objects done after the attestation date and in the date ranges
     * @throws ParseException - if there is an issue parsing the dates
     */
    public static List<ExplanationOfBenefit> filterByDate(List<ExplanationOfBenefit> benes,
                                              Date attestationDate,
                                              List<DateRange> dateRanges) throws ParseException {
        if (benes == null || benes.isEmpty()) {
            return new ArrayList<>();
        }
        return benes.stream().filter(b -> valid(b, attestationDate, dateRanges)).collect(Collectors.toList());
    }

    public static boolean valid(ExplanationOfBenefit bene, Date attestationDate, List<DateRange> dateRanges) {
        if (bene == null) {
            return false;
        }
        try {
            if (afterAttestation(attestationDate, bene)) {
                for (DateRange r : dateRanges) {
                    if (withinDateRange(bene, r)) {
                        return true;
                    }
                }
            }
        } catch (Exception ex) {
            return false;
        }
        return false;
    }

    /**
     * True if the submitted date is after attestation date. This takes the attestation date
     * and zeros out time so that we can assume if the attestation date is 10/01/2020 23:59:59
     * and the billable end time is 10/01/2020 00:00:00 it will be included because they were on
     * the same day
     *
     * @param attestation - attestation date
     * @param ben - the explanation of benefit object
     * @return if the EOB object is after the attestation date
     * @throws ParseException - if there is an issue parsing the dates
     */
    static boolean afterAttestation(Date attestation, ExplanationOfBenefit ben) throws ParseException {
        SimpleDateFormat fullDateFormat = new SimpleDateFormat(FULL);
        SimpleDateFormat shortDateFormat = new SimpleDateFormat(SHORT);
        if (ben == null || ben.getBillablePeriod() == null || attestation == null) {
            return false;
        }
        Date attToUse = fullDateFormat.parse(shortDateFormat.format(attestation) + " 00:00:00");
        Period p = ben.getBillablePeriod();
        Date end = p.getEnd();
        return end != null && end.getTime() >= attToUse.getTime();
    }

    /**
     * Returns true if the EOB object is within a date range
     * @param ben - the EOB object
     * @param range - the date range
     * @return true if the EOB object's billable period is within the date range
     * @throws ParseException - if there was a date parsing error
     */
    static boolean withinDateRange(ExplanationOfBenefit ben, DateRange range) throws ParseException {
        if (ben == null || ben.getBillablePeriod() == null) {
            return false;
        }
        Period p = ben.getBillablePeriod();
        Date start = p.getStart();
        Date end = p.getEnd();
        if (start == null || end == null) {
            return false;
        }
        return range.inRange(start) || range.inRange(end);
    }
}
