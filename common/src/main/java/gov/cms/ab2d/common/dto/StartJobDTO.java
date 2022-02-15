package gov.cms.ab2d.common.dto;

import gov.cms.ab2d.fhir.FhirVersion;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
public class StartJobDTO {
    private final String contractNumber;
    private final String organization;// NOSONAR - not quite in use yet
    private final String resourceTypes;
    private final String url;
    private final String outputFormat;
    private final OffsetDateTime since;
    @NotNull
    private final FhirVersion version;
}
