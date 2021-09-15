package gov.cms.ab2d.eventlogger.events;

import gov.cms.ab2d.eventlogger.LoggableEvent;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * When beneficiaries EOB claims are searched from BFD. This is a summary object. Otherwise, we're creating
 * an event every time a beneficiary is searched which is probably too much data to log
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ContractSearchEvent extends LoggableEvent {
    // The contract searched
    private String contractNumber;

    // The number of beneficiaries in the contract
    private int benesExpected;

    // The number of benes queued to make requests for
    private int benesQueued;

    // The number successfully searched
    private int benesSearched;

    // The number of errors that occured during searching
    private int benesErrored;

    // The number of eobs created
    private int eobsFetched;

    // The number of eobs written
    private int eobsWritten;

    // The number of eob files created
    private int eobFiles;

    public ContractSearchEvent() { }

    @SuppressWarnings("ParameterNumber")
    public ContractSearchEvent(String organization, String jobId, String contractNumber, int benesExpected,
                               int benesQueued, int benesSearched, int benesErrors,
                               int eobsFetched, int eobsWritten, int eobFiles) {
        super(OffsetDateTime.now(), organization, jobId);
        this.contractNumber = contractNumber;

        this.benesExpected = benesExpected;
        this.benesQueued = benesQueued;
        this.benesSearched = benesSearched;
        this.benesErrored = benesErrors;

        this.eobsFetched = eobsFetched;
        this.eobsWritten = eobsWritten;
        this.eobFiles = eobFiles;
    }

    @Override
    public String asMessage() {
        return String.format("(%s) %s number in contract %s", getJobId(), contractNumber, benesExpected);
    }
}
