package gov.cms.ab2d.contracts.hmsapi;

import gov.cms.ab2d.contracts.model.Contract;
import lombok.*;

@NoArgsConstructor  // Needed for Jackson
@AllArgsConstructor
@Data
public class HPMSOrganizationInfo {

    private String parentOrgName;
    private Integer parentOrgId;
    private String contractId;
    private String contractName;
    private String orgMarketingName;
}
