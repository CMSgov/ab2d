package gov.cms.ab2d.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.List;

import gov.cms.ab2d.fhir.Versions.FhirVersions;

/**
 * Misc methods to support different versions of FHIR
 */
@Slf4j
public final class FHIRUtil {

    private FHIRUtil() { }

    /**
     * Given a resource and a FHIR version, convert the object to JSON
     *
     * @param operationOutcome - the resource
     * @param version - the FHIR version
     * @return the JSON string
     */
    public static String outcomeToJSON(IBaseResource operationOutcome, FhirVersions version) {
        FhirContext ctx = Versions.getContextFromVersion(version);
        IParser jsonParser = ctx.newJsonParser();
        jsonParser.setPrettyPrint(true);
        return jsonParser.encodeResourceToString(operationOutcome);
    }

    /**
     * If there is an error, return the FHIR error for the correct version
     *
     * @param msg - the error message
     * @param version - the FHIR version
     * @return the OperationOutcome object
     */
    public static IBaseResource getErrorOutcome(String msg, FhirVersions version) {
        IBaseResource operationOutcome = (IBaseResource) Versions.getObject(version, "OperationOutcome");
        List issues = (List) Versions.invokeGetMethod(operationOutcome, "getIssue");

        Object newIssue = Versions.instantiateClass(version, "OperationOutcome", "OperationOutcomeIssueComponent");

        Object severityError = Versions.instantiateEnum(version, "OperationOutcome", "IssueSeverity", "ERROR");
        Versions.invokeSetMethod(newIssue, "setSeverity", severityError, severityError.getClass());

        Object issueTypeInvalid = Versions.instantiateEnum(version, "OperationOutcome", "IssueType", "INVALID");
        Versions.invokeSetMethod(newIssue, "setCode", issueTypeInvalid, issueTypeInvalid.getClass());
        Object codableConcept = Versions.getObject(version, "CodeableConcept");
        Versions.invokeSetMethod(codableConcept, "setText", msg, String.class);
        Versions.invokeSetMethod(newIssue, "setDetails", codableConcept, codableConcept.getClass());

        issues.add(newIssue);
        Versions.invokeSetMethod(operationOutcome, "setIssue", issues, List.class);

        return operationOutcome;
    }
}
