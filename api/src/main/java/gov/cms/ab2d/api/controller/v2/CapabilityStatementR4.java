package gov.cms.ab2d.api.controller.v2;

import gov.cms.ab2d.api.controller.common.AB2DCapabilityStatementFactory;
import org.hl7.fhir.r4.model.CapabilityStatement;

public class CapabilityStatementR4 {
    public static CapabilityStatement populateCS(String server) {
        CapabilityStatement cs = AB2DCapabilityStatementFactory.generateR4CapabilityStatement(server);
        return cs;
    }
}
