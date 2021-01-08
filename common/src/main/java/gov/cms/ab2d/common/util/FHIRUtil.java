package gov.cms.ab2d.common.util;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public final class FHIRUtil {

    private FHIRUtil() {
    }

    public static String outcomeToJSON(org.hl7.fhir.dstu3.model.OperationOutcome operationOutcome) {
        FhirContext ctx = FhirContext.forDstu3();
        IParser jsonParser = ctx.newJsonParser();
        jsonParser.setPrettyPrint(true);
        return jsonParser.encodeResourceToString(operationOutcome);
    }

    public static org.hl7.fhir.dstu3.model.OperationOutcome getErrorOutcome(String msg) {
        org.hl7.fhir.dstu3.model.OperationOutcome operationOutcome = new org.hl7.fhir.dstu3.model.OperationOutcome();
        operationOutcome.addIssue()
                .setSeverity(org.hl7.fhir.dstu3.model.OperationOutcome.IssueSeverity.ERROR)
                .setCode(org.hl7.fhir.dstu3.model.OperationOutcome.IssueType.INVALID)
                .setDetails(new org.hl7.fhir.dstu3.model.CodeableConcept()
                        .setText(msg));
        return operationOutcome;
    }
}
