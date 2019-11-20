package gov.cms.ab2d.common.util;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.OperationOutcome;

public final class FHIRUtil {

    private FHIRUtil() {
    }

    public static String outcomeToJSON(OperationOutcome operationOutcome) {
        FhirContext ctx = FhirContext.forDstu3();
        IParser jsonParser = ctx.newJsonParser();
        jsonParser.setPrettyPrint(true);
        return jsonParser.encodeResourceToString(operationOutcome);
    }

    public static OperationOutcome getErrorOutcome(String msg) {
        OperationOutcome operationOutcome = new OperationOutcome();
        operationOutcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(OperationOutcome.IssueType.INVALID)
                .setDetails(new CodeableConcept()
                        .setText(msg));
        return operationOutcome;
    }
}
