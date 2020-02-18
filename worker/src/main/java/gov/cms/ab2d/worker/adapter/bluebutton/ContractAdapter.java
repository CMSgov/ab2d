package gov.cms.ab2d.worker.adapter.bluebutton;

public interface ContractAdapter {

    GetPatientsByContractResponse getPatients(String contractNumber, int currentMonth);
}
