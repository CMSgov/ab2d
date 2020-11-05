package gov.cms.ab2d.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CoverageMembership {
    private String beneficiaryId;
    private String mbiId;
    private int year;
    private int month;
}
