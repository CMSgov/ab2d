package gov.cms.ab2d.worker;

import gov.cms.ab2d.filter.FilterOutByDate;

public final class TestUtil {

    private TestUtil() {
    }

    public static FilterOutByDate.DateRange getOpenRange() {
        return FilterOutByDate.getDateRange(1, 2019, 12, 2030);
    }
}
