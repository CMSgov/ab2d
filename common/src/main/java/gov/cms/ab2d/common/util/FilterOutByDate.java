package gov.cms.ab2d.common.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Period;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static gov.cms.ab2d.common.util.DateUtil.AB2D_ZONE;

/**
 * Utility class to take different subscription ranges and attestation dates and
 * determine if an explanation of benefit object should be filtered out based on
 * those dates. Older Date objects are used instead of more modern dates because the
 * explanation of benefit object billing dates periods are in dates. For consistency
 * and to minimize time zone issues, we kept everything as Date objects.
 */
public final class FilterOutByDate {
    private static final String SHORT = "MM/dd/yyyy";
    private static final String FULL = "MM/dd/yyyy HH:mm:ss:SSS";

    public static final TimeZone TIMEZONE = TimeZone.getTimeZone(AB2D_ZONE);

    /**
     * Date range class used to define a from and to date for a subscribers membership.
     * We only deal with date ranges consisting of months and eliminate the ability to construct other
     * ranges. A date range is from the start of a month to the end of that same or other month only.
     */
    @SuppressFBWarnings
    @Getter
    public static final class DateRange {

        private final Date start;
        private final Date end;

        /**
         * Create date range from the beginning of the startMonth during startYear to the end of the endMonth during endYear
         */
        private DateRange(int startMonth, int startYear, int endMonth, int endYear) {
            this.start = getStartOfMonth(startMonth, startYear);
            this.end = getEndOfMonth(endMonth, endYear);
        }

        /**
         * True if a date is in range between the start date and the end date.
         *
         * This inRange function is inclusive meaning if the end date is May 31st at 11:59:59.999 and someone
         * enters that date in, the function will return true. But if someone enters June 1st at exactly midnight
         * it will return false.
         *
         * @param d - the date to compare
         * @return true if the date is in range
         */
        public boolean inRange(Date d) {
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
     */
    public static List<DateRange> getDateRanges(List<Integer> months, int year) {
        if (months == null || months.isEmpty() || year == 0) {
            return new ArrayList<>();
        }
        List<Integer> monthList = getMonthList(months);
        return getRanges(monthList, year);
    }

    private static List<DateRange> getRanges(List<Integer> monthList, int year) {
        List<DateRange> ranges = new ArrayList<>();
        int startVal = -1;
        int endVal = -1;
        boolean in = false;
        // Iterate through the month list and create data ranges
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
     * Create a list of size 12 of months where if the passed months argument is in the list, set it to 1.
     * For example, if you have a list of months [1, 4, 5] for months Jan, Apr & May, convert into list:
     * [1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0]
     *
     * @param months - the months
     * @return the months in a list of 12 where a value of 1 means that month is checked
     */
    private static List<Integer> getMonthList(List<Integer> months) {
        List<Integer> monthList = new ArrayList<>(12);
        for (int i = 0; i < 12; i++) {
            monthList.add(0);
        }
        for (int i = 0; i < months.size(); i++) {
            int m = months.get(i);
            monthList.set(m - 1, 1);
        }
        months.forEach(c -> monthList.set(c - 1, 1));
        return monthList;
    }

    /**
     * Given a month and year, get us the Date that is at the beginning of the month. For example,
     * 10, 2020 = 10/01/2020 00:00:00
     *
     * @param month - month to choose
     * @param year  - year to choose
     * @return the beginning of the month
     */
    public static Date getStartOfMonth(int month, int year) {
        Calendar c = Calendar.getInstance();
        c.setTimeZone(TIMEZONE);
        c.set(getYearToUse(year), month - 1, 1, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
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
    public static Date getEndOfMonth(int month, int year) {
        Calendar c = Calendar.getInstance();
        c.setTimeZone(TIMEZONE);
        c.set(getYearToUse(year), month - 1, 1, 23, 59, 59);
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        c.set(Calendar.MILLISECOND, 999);
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
    public static DateRange getDateRange(int startMonth, int startYear, int endMonth, int endYear) {
        return new DateRange(startMonth, startYear, endMonth, endYear);
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
    public static DateRange getDateRange(int month, int year) {
        return new DateRange(month, year, month, year);
    }

    /**
     * This does most of the work of the class. It takes a list of explanation of benefit objects,
     * the attestation date and list of valid date ranges and returns the list of qualifying objects
     *
     * @param benes           - the explanation of benefit objects
     * @param attestationDate - the attestation date
     * @param earliestDate    - the earliest date that ab2d data is available for any PDP
     * @param dateRanges      - the list of date ranges
     * @return - the list of objects done after the attestation date and in the date ranges
     * @throws ParseException - if there is an issue parsing the dates
     */
    public static List<ExplanationOfBenefit> filterByDate(List<ExplanationOfBenefit> benes,
                                              Date attestationDate,
                                              Date earliestDate,
                                              List<DateRange> dateRanges) {
        if (benes == null || benes.isEmpty()) {
            return new ArrayList<>();
        }
        return benes.stream().filter(b -> valid(b, attestationDate, earliestDate, dateRanges)).collect(Collectors.toList());
    }

    public static boolean valid(ExplanationOfBenefit bene, Date attestationDate, Date earliestDate, List<DateRange> dateRanges) {
        if (bene == null) {
            return false;
        }
        try {
            if (afterDate(attestationDate, bene) && afterDate(earliestDate, bene)) {
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
     * @param dateVal - attestation date
     * @param ben - the explanation of benefit object
     * @return if the EOB object is after the attestation date
     * @throws ParseException - if there is an issue parsing the dates
     */
    static boolean afterDate(Date dateVal, ExplanationOfBenefit ben) throws ParseException {
        SimpleDateFormat fullDateFormat = new SimpleDateFormat(FULL);
        SimpleDateFormat shortDateFormat = new SimpleDateFormat(SHORT);
        if (ben == null || ben.getBillablePeriod() == null || dateVal == null) {
            return false;
        }

        Date attToUse = fullDateFormat.parse(shortDateFormat.format(dateVal) + " 00:00:00:000");
        Period p = ben.getBillablePeriod();
        Date end = p.getEnd();
        return end != null && end.getTime() >= attToUse.getTime();
    }

    /**
     * Returns true if the EOB object is within a date range
     * @param ben - the EOB object
     * @param range - the date range
     * @return true if the EOB object's billable period is within the date range
     */
    static boolean withinDateRange(ExplanationOfBenefit ben, DateRange range) {
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
