package gov.cms.ab2d.hpms.hmsapi;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Sponsor;
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

    public boolean hasChanges(Contract contract) {
        return contract.hasChanges(contractName, parentOrgId.longValue(), parentOrgName, orgMarketingName);
    }

    public Contract build(Sponsor savedSponsor) {
        return new Contract(contractId, contractName, parentOrgId.longValue(),
                parentOrgName, orgMarketingName, savedSponsor);
    }

    public Contract updateContract(Contract contract) {
        return contract.updateOrg(contractName, parentOrgId.longValue(), parentOrgName, orgMarketingName);
    }
}
