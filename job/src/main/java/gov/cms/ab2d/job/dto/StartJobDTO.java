package gov.cms.ab2d.job.dto;

import gov.cms.ab2d.fhir.FhirVersion;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/**
 * StartJobDTO is a fully verified and validated request to start a job.
 *
 * This means the caller has the permission to run against this contract and that the contract is attested.
 */
@Data
@AllArgsConstructor
public class StartJobDTO {
    @NotNull
    private final String contractNumber;
    @NotNull
    private final String organization;
    @NotNull
    private final String resourceTypes;
    @NotNull
    private final String url;
    @NotNull
    private final String outputFormat;
    private final OffsetDateTime since;
    private final OffsetDateTime until;
    @NotNull
    private final FhirVersion version;
}
