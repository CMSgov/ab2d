package gov.cms.ab2d.hpms.hmsapi;

import lombok.Data;

import java.util.Set;
import javax.validation.constraints.NotNull;

/**
 * Holder class for what is returned by the HMS Organizations endpoint
 */
@Data
public class HMSOrganizations {

    @NotNull
    private Set<HMSOrganizationInfo> orgs;
}
