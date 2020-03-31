package gov.cms.ab2d.eventlogger.events;

import gov.cms.ab2d.eventlogger.LoggableEvent;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * When beneficiaries EOB claims are searched from BFD. This is a summary object. Otherwise, we're creating
 * an event every time a beneficiary is searched which is probably too much data to log
 */
@Data
public class ContractBeneSearchEvent extends LoggableEvent {
    // The contract searched
    private String contractNumber;
    // The number of beneficiaries in the contract
    private int numInContract;
    // The number successfully searched
    private int numSearched;
    // The number of beneficiaries who opted out
    private int numOptedOut;
    // The number of errors that occured during searching
    private int numErrors;

    public ContractBeneSearchEvent(String user, String jobId, String contractNumber, int numInContract, int numbSearched,
                                   int numOptedOut, int numErrors) {
        super(OffsetDateTime.now(), user, jobId);
        this.contractNumber = contractNumber;
        this.numSearched = numbSearched;
        this.numInContract = numInContract;
        this.numOptedOut = numOptedOut;
        this.numErrors = numErrors;
    }
}
