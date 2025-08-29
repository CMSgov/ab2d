package gov.cms.ab2d.contracts.model;

import java.time.OffsetDateTime;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContractDTO {
    @NotNull
    private Long id;

    @NotNull
    private String contractNumber;

    @NotNull
    private String contractName;

    @EqualsAndHashCode.Exclude  // contractNumber is sufficient, breaks on Windows due sub-seconds not matching
    private OffsetDateTime attestedOn;

    private Contract.ContractType contractType;

    private Integer totalEnrollment;

    private Integer medicareEligible;

    public boolean hasDateIssue() {
        return Contract.ContractType.CLASSIC_TEST == contractType;
    }
}
