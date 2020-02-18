package gov.cms.ab2d.worker.adapter.bluebutton;

import gov.cms.ab2d.filter.FilterOutByDate.DateRange;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.ArrayList;
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

        @Builder.Default
        private List<DateRange> dateRangesUnderContract = new ArrayList<>();

    }
}
