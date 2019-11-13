package gov.cms.ab2d.worker.adapter.bluebutton;

public interface BeneficiaryAdapter {

    GetPatientsByContractResponse getPatientsByContract(String contractNumber);
}
