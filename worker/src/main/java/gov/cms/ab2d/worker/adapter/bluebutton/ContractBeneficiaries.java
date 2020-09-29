package gov.cms.ab2d.worker.adapter.bluebutton;

import gov.cms.ab2d.common.util.FilterOutByDate.DateRange;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractBeneficiaries {

    private String contractNumber;

    @Singular
    private Map<String, PatientDTO> patients;

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
