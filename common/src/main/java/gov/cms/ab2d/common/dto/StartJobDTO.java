package gov.cms.ab2d.common.dto;

import gov.cms.ab2d.fhir.FhirVersion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartJobDTO {
    private String contractNumber;
    private String organization;
    private String resourceTypes;
    private String url;
    private String outputFormat;
    private OffsetDateTime since;
    @NotNull
    private FhirVersion version;
}
