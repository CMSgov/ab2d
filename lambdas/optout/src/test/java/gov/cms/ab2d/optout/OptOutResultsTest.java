package gov.cms.ab2d.optout;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OptOutResultsTest {
    
    @Test 
    public void itStoresValuesCorrectly() {
        int todayOptIn = 1;
        int todayOptOut = 1;

        int totalOptedIn = 1;
        int totalOptedOut = 1;
        OptOutResults optOutResults = new OptOutResults(todayOptIn, todayOptOut, totalOptedIn, totalOptedOut);

        assertEquals(todayOptIn, optOutResults.getOptInToday());
        assertEquals(todayOptOut, optOutResults.getOptOutToday());
        assertEquals(totalOptedIn, optOutResults.getOptInTotal());
        assertEquals(totalOptedOut, optOutResults.getOptOutTotal());
        assertEquals(todayOptIn + todayOptOut, optOutResults.getTotalToday());
        assertEquals(totalOptedIn + totalOptedOut, optOutResults.getTotalAllTime());
    }

}
