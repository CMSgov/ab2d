package gov.cms.ab2d.optout;


public enum RecordStatus {

    ACCEPTED("Accepted  00"),
    REJECTED("Rejected  02");
    private final String status;
    RecordStatus(final String status) {
        this.status = status;
    }
    @Override
    public String toString() {
        return status;
    }

}

