package gov.cms.ab2d.bfd.client;

import lombok.AllArgsConstructor;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * BFDSearchDTO is a fully verified and validated request to search BFD.
 */
@Data
@AllArgsConstructor
public class BFDSearchDTO {
    @NotNull
    private final long patientId;
    @NotNull
    private final FhirVersion version;
    @NotNull
    private final String contractNum;
    @NotNull
    private final String bulkJobId;
    @NotNull
    private final int pageSize;
    private final OffsetDateTime since;
    private final OffsetDateTime until;
    private final List<String> serviceDates;
}
