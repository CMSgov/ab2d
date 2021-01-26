package gov.cms.ab2d.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.List;

@Slf4j
public final class FHIRUtil {

    private FHIRUtil() {
    }

    public static String outcomeToJSON(IBaseResource operationOutcome, Versions.FHIR_VERSIONS version) {
        FhirContext ctx = Versions.getContextFromVersion(version);
        IParser jsonParser = ctx.newJsonParser();
        jsonParser.setPrettyPrint(true);
        return jsonParser.encodeResourceToString(operationOutcome);
    }

    public static IBaseResource getErrorOutcome(String msg, Versions.FHIR_VERSIONS version) {
        try {
            IBaseResource operationOutcome = (IBaseResource) Versions.instantiateClass(version, "OperationOutcome");
            List issues = (List) Versions.invokeGetMethod(operationOutcome, "getIssue");

            Object newIssue = Versions.instantiateClass(version, "OperationOutcome", "OperationOutcomeIssueComponent");

            Object severityError = Versions.instantiateEnum(version, "OperationOutcome", "IssueSeverity", "ERROR");
            Versions.invokeSetMethod(newIssue, "setSeverity", severityError, severityError.getClass());

            Object issueTypeInvalid = Versions.instantiateEnum(version, "OperationOutcome", "IssueType", "INVALID");
            Versions.invokeSetMethod(newIssue, "setCode", issueTypeInvalid, issueTypeInvalid.getClass());
            Object codableConcept = Versions.instantiateClass(version, "CodeableConcept");
            Versions.invokeSetMethod(codableConcept, "setText", msg, String.class);
            Versions.invokeSetMethod(newIssue, "setDetails", codableConcept, codableConcept.getClass());

            issues.add(newIssue);
            Versions.invokeSetMethod(operationOutcome, "setIssue", issues, List.class);

            return operationOutcome;
        } catch (Exception ex) {
            log.error("Unable to create error outcome with message: " + msg, ex);
            return null;
        }
    }
}
