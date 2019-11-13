package gov.cms.ab2d.worker.adapter.bluebutton;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetPatientsByContractResponse {

    private String contractNumber;

    @Singular
    private List<PatientDTO> patients;



    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientDTO {
        private String patientId;

        @Singular("monthUnderContract")
        private List<Integer> monthsUnderContract;
    }
}
