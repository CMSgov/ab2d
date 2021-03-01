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
public class ContractBeneSearchEvent extends LoggableEvent {
    // The contract searched
    private String contractNumber;
    // The number of beneficiaries in the contract
    private int numInContract;
    // The number successfully searched
    private int numSearched;
    // The number of errors that occured during searching
    private int numErrors;

    public ContractBeneSearchEvent() { }

    public ContractBeneSearchEvent(String organization, String jobId, String contractNumber, int numInContract, int numbSearched,
                                   int numErrors) {
        super(OffsetDateTime.now(), organization, jobId);
        this.contractNumber = contractNumber;
        this.numSearched = numbSearched;
        this.numInContract = numInContract;
        this.numErrors = numErrors;
    }
}
