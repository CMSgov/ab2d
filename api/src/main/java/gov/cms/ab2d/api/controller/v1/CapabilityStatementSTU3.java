package gov.cms.ab2d.api.controller.v1;

import gov.cms.ab2d.api.controller.common.AB2DCapabilityStatementFactory;
import org.hl7.fhir.dstu3.model.CapabilityStatement;

public class CapabilityStatementSTU3 {
    public static CapabilityStatement populateCS(String server) {
        CapabilityStatement cs = AB2DCapabilityStatementFactory.generateSTU3CapabilityStatement(server);
        return cs;
    }
}
