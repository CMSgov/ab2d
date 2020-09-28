package gov.cms.ab2d.hpms.hmsapi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
