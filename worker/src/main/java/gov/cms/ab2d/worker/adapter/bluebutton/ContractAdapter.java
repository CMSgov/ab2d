package gov.cms.ab2d.worker.adapter.bluebutton;

public interface ContractAdapter {

    ContractBeneficiaries getPatients(String contractNumber, int currentMonth);
}
