package gov.cms.ab2d.api.controller.common;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * CheckValidCreateJobDTO is used to pass $export parameters to the validation function
 */
@Data
@AllArgsConstructor
public class CheckValidParametersDTO {
    @NotNull
    private final String resourceTypes;
    @NotNull
    private final String outputFormat;
    private final OffsetDateTime since;
    private final OffsetDateTime until;
    private final List<String> serviceDates;
}
