package gov.cms.ab2d.hpms.hmsapi;

import lombok.Data;

@Data
public class HMSOrganizationInfo {

    private String parentOrgName;
    private Integer parentOrgId;

    private String contractId;
    private String contractName;
    private String orgMarketingName;
}
