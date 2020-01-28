package gov.cms.ab2d.worker.processor.domainmodel;

import gov.cms.ab2d.worker.adapter.bluebutton.GetPatientsByContractResponse.PatientDTO;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class JobDM {

    private final Long jobId;
    private final String jobUuid;

    @Singular
    private final List<ContractDM> contracts;


    @Getter
    @Builder
    public static class ContractDM {
        private final Long contractId;
        private final String contractNumber;
        private Map<Integer, List<PatientDTO>> slices;


        public long getPatientCountInContract() {
            return getSlices().entrySet().stream()
                    .map(entry -> entry.getValue())
                    .mapToInt(patients -> patients.size())
                    .sum();
        }

    }


    public ContractDM getContractDM(String contractNumber) {
        return getContracts().stream()
                .filter(contractDM -> contractDM.getContractNumber().equals(contractNumber))
                .findFirst()
                .get();
    }


}