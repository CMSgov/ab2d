package gov.cms.ab2d.worker.adapter.bluebutton;

import java.util.concurrent.ExecutionException;

public interface ContractBeneSearch {

    ContractBeneficiaries getPatients(String contractNumber, int currentMonth) throws ExecutionException, InterruptedException;
}
