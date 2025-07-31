package gov.cms.ab2d.optout;

public class OptOutInformation {
    private final String mbi;
    private final boolean optOutFlag;
    public OptOutInformation(String mbi, boolean optOutFlag) {
        this.mbi = mbi;
        this.optOutFlag = optOutFlag;
    }
    public boolean getOptOutFlag() {
        return optOutFlag;
    }
    public String getMbi() {
        return mbi;
    }

}