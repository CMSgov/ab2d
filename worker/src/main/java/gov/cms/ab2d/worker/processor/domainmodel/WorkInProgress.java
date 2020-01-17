package gov.cms.ab2d.worker.processor.domainmodel;

import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse.PatientDTO;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class WorkInProgress {

    // Need to extract another inner DTO
//    private final List<ContractPatientsDTO> contractsWithPatients;
    private final String contractNumber;
    private final List<PatientDTO> patients;
    private int processedCount;

    public void incrementProcessedCount() {
        ++processedCount;
    }

}