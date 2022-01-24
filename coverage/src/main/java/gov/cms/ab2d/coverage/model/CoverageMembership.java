package gov.cms.ab2d.coverage.model;

import gov.cms.ab2d.common.model.Identifiers;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CoverageMembership {
    private Identifiers identifiers;
    private int year;
    private int month;
}
