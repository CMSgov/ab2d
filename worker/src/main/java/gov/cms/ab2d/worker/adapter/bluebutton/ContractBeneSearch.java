package gov.cms.ab2d.worker.adapter.bluebutton;

import gov.cms.ab2d.worker.processor.domainmodel.ProgressTracker;

import java.util.concurrent.ExecutionException;

public interface ContractBeneSearch {

    ContractBeneficiaries getPatients(String contractNumber, int currentMonth, ProgressTracker tracker) throws ExecutionException, InterruptedException;
}
