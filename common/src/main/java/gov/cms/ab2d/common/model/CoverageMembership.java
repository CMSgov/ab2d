package gov.cms.ab2d.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CoverageMembership {
    private String beneficiaryId;
    private int year;
    private int month;
}
