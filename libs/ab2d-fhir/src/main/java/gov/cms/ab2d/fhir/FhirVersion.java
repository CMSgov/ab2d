package gov.cms.ab2d.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.EncodingEnum;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public enum FhirVersion {
    STU3("org.hl7.fhir.dstu3.model", FhirContext.forDstu3(), FhirVersionEnum.DSTU3, "/v1/", "/v1/",
            org.hl7.fhir.dstu3.model.ResourceType.Patient, false),
    R4("org.hl7.fhir.r4.model", FhirContext.forR4(), FhirVersionEnum.R4, "/v2/", "/v2/",
            org.hl7.fhir.r4.model.ResourceType.Patient, true);

    private final String classLocation;
    private final FhirContext context;
    private final FhirVersionEnum fhirVersionEnum;
    private final String ab2dVersionString;
    private final String bfdVersionString;
    private final Object patientEnum;
    private final boolean supportsDefaultSince;

    FhirVersion(String classLocation, FhirContext context, FhirVersionEnum fhirVersionEnum, String ab2dVersionString, String bfdVersionString,
                Object patientEnum, boolean supportsDefaultSince) {
        this.classLocation = classLocation;
        this.context = context;
        this.fhirVersionEnum = fhirVersionEnum;
        this.ab2dVersionString = ab2dVersionString;
        this.bfdVersionString = bfdVersionString;
        this.patientEnum = patientEnum;
        this.supportsDefaultSince = supportsDefaultSince;
    }

    public boolean supportDefaultSince() {
        return this.supportsDefaultSince;
    }

    public IParser getJsonParser() {
        EncodingEnum respType = EncodingEnum.forContentType(EncodingEnum.JSON_PLAIN_STRING);
        return respType.newParser(FhirContext.forCached(fhirVersionEnum));
    }

    public static FhirVersion from(FhirVersionEnum fhirVersionEnum) {
        return Stream.of(FhirVersion.values())
                .filter(fhir -> fhir.fhirVersionEnum == fhirVersionEnum)
                .findFirst().orElse(null);
    }

    public FhirContext getContext() {
        return this.context;
    }

    public static FhirVersion fromAB2DUrl(String url) {
        return Stream.of(FhirVersion.values())
                .filter(fhir -> url.contains(fhir.ab2dVersionString))
                .findFirst().orElse(null);
    }

    public String getBfdVersionString() {
        return bfdVersionString;
    }

    public String getClassName(String name) {
        if (this.classLocation == null) {
            return null;
        }
        return this.classLocation + "." + name;
    }

    public Class<?> getClassFromName(String name) {
        try {
            return Class.forName(getClassName(name));
        } catch (Exception e) {
            log.error(String.format("Unable to create a class from name: %s", name), e);
            return null;
        }
    }

    public Class<? extends IBaseBundle> getBundleClass() {
        try {
            return (Class<? extends IBaseBundle>) Class.forName(this.getClassName("Bundle"));
        } catch (Exception e) {
            log.error("Unable to get the right class for Bundle", e);
            return null;
        }
    }

    public Class<? extends IBaseResource> getPatientClass() {
        try {
            return (Class<? extends IBaseResource>) Class.forName(this.getClassName("Patient"));
        } catch (Exception e) {
            log.error("Unable to get the right class for Patient", e);
            return null;
        }
    }

    public String getFhirTime(OffsetDateTime dateTime) {
        Object dt = Versions.getObject(this, "DateTimeType", dateTime.toString(), String.class);
        return (String) Versions.invokeGetMethod(dt, "toHumanDisplay");
    }

    /**
     * returns if metadata.getStatus() == ACTIVE to verify that the service is active
     *
     * @param resource - the meta data resource
     * @return true if the value is ACTIVE
     */
    public boolean metaDataValid(IBaseConformance resource) {
        if (resource == null) {
            return false;
        }
        Object val = Versions.invokeGetMethod(resource, "getStatus");
        Object activeEnum = Versions.instantiateEnum(this, "Enumerations", "PublicationStatus", "ACTIVE");
        return val == activeEnum;
    }

    /**
     * Return the correct CapabilityStatement object for the correct version
     *
     * @return the object
     */
    @SuppressWarnings("unchecked")
    public Class<? extends IBaseConformance> getCapabilityClass() {
        try {
            return (Class<? extends IBaseConformance>) Class.forName(this.getClassName("CapabilityStatement"));
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Given a resource and a FHIR version, convert the object to JSON
     *
     * @param operationOutcome - the resource
     * @return the JSON string
     */
    public String outcomePrettyToJSON(IBaseResource operationOutcome) {
        IParser jsonParser = this.getJsonParser();
        jsonParser.setPrettyPrint(true);
        String val = jsonParser.encodeResourceToString(operationOutcome);
        jsonParser.setPrettyPrint(false);
        return val;
    }

    /**
     * If there is an error, return the FHIR error for the correct version
     *
     * @param msg - the error message
     * @return the OperationOutcome object
     */
    public IBaseResource getErrorOutcome(String msg) {
        String operationOutcomeClassName = "OperationOutcome";

        IBaseResource operationOutcome = (IBaseResource) Versions.getObject(this, operationOutcomeClassName);
        List issues = (List) Versions.invokeGetMethod(operationOutcome, "getIssue");

        Object newIssue = Versions.instantiateClass(this, operationOutcomeClassName, "OperationOutcomeIssueComponent");

        Object severityError = Versions.instantiateEnum(this, operationOutcomeClassName, "IssueSeverity", "ERROR");
        Versions.invokeSetMethod(newIssue, "setSeverity", severityError, severityError.getClass());

        Object issueTypeInvalid = Versions.instantiateEnum(this, operationOutcomeClassName, "IssueType", "INVALID");
        Versions.invokeSetMethod(newIssue, "setCode", issueTypeInvalid, issueTypeInvalid.getClass());
        Object codableConcept = Versions.getObject(this, "CodeableConcept");
        Versions.invokeSetMethod(codableConcept, "setText", msg, String.class);
        Versions.invokeSetMethod(newIssue, "setDetails", codableConcept, codableConcept.getClass());

        issues.add(newIssue);
        Versions.invokeSetMethod(operationOutcome, "setIssue", issues, List.class);

        return operationOutcome;
    }

    public Object getPatientEnum() {
        return patientEnum;
    }
}
