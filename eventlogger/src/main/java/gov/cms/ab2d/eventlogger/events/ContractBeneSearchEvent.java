package gov.cms.ab2d.eventlogger.events;

import gov.cms.ab2d.eventlogger.LoggableEvent;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * When beneficiaries EOB claims are searched from BFD. This is a summary object. Otherwise, we're creating
 * an event every time a beneficiary is searched which is probably too much data to log
 */
@Data
@AllArgsConstructor
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
}
