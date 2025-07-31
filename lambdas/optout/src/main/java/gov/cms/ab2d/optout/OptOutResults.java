package gov.cms.ab2d.optout;

public class OptOutResults {

    private final int optInToday;
    private final int optOutToday;
    private final int optInTotal;
    private final int optOutTotal;

    public OptOutResults(int optInToday, int optOutToday, int optInTotal, int optOutTotal) {
        this.optInToday = optInToday;
        this.optOutToday = optOutToday;
        this.optInTotal = optInTotal;
        this.optOutTotal = optOutTotal;
    }

    public int getOptInToday() {
        return optInToday;
    }

    public int getOptOutToday() {
        return optOutToday;
    }

    public int getTotalToday() {
        return optInToday + optOutToday;
    }

    public int getOptInTotal() {
        return optInTotal;
    }

    public int getOptOutTotal() {
        return optOutTotal;
    }

    public int getTotalAllTime() {
        return optInTotal + optOutTotal;
    }

}
