package gov.cms.ab2d.worker.processor.domainmodel;

import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse.PatientDTO;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
@Getter
@Builder
public class ProgressTracker {

    private final String jobUuid;

    @Singular
    private final List<ContractDM> contracts;


    @Getter
    @Builder
    public static class ContractDM {
        private final Long contractId;
        private final String contractNumber;
        private Map<Integer, List<PatientDTO>> slices;

    }


}