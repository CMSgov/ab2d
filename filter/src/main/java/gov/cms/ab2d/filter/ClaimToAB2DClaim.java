package gov.cms.ab2d.filter;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hl7.fhir.dstu3.model.Claim;

public class ClaimToAB2DClaim {
    public static AB2DClaim from(Claim claim) {
        if (claim == null) {
            throw new ResourceNotFoundException("No claim information provided");
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        AB2DClaim ab2dClaim = mapper.convertValue(claim, AB2DClaim.class);
        return ab2dClaim;
    }
}
