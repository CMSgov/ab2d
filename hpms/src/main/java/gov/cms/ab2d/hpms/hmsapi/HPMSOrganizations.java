package gov.cms.ab2d.hpms.hmsapi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Holder class for what is returned by the HMS Organizations endpoint
 */
@NoArgsConstructor  // Needed for Jackson
@AllArgsConstructor
@Data
public class HPMSOrganizations {

    @NotNull
    private List<HPMSOrganizationInfo> orgs;
}
