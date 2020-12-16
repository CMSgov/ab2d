package gov.cms.ab2d.hpms.hmsapi;

import gov.cms.ab2d.common.model.Contract;
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

    public boolean hasChanges(Contract contract) {
        return contract.hasChanges(contractName, parentOrgId.longValue(), parentOrgName, orgMarketingName);
    }

    public Contract build() {
        return new Contract(contractId, contractName, parentOrgId.longValue(),
                parentOrgName, orgMarketingName);
    }

    public Contract updateContract(Contract contract) {
        return contract.updateOrg(contractName, parentOrgId.longValue(), parentOrgName, orgMarketingName);
    }
}
