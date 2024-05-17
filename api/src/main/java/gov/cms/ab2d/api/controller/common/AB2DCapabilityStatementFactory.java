package gov.cms.ab2d.api.controller.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ca.uhn.fhir.context.FhirVersionEnum;
import static gov.cms.ab2d.api.controller.common.ApiText.APPLICATION_JSON;


public class AB2DCapabilityStatementFactory {
    static final String oauthString = "OAuth";

    static SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    static SimpleDateFormat sdfLess = new SimpleDateFormat("MM/dd/yyyy HH:mm");

    public static org.hl7.fhir.r4.model.CapabilityStatement generateR4CapabilityStatement(String server) {
        // R4
        org.hl7.fhir.r4.model.CodeType codeType = new org.hl7.fhir.r4.model.CodeType();        
        org.hl7.fhir.r4.model.CodeType codeType2 = new org.hl7.fhir.r4.model.CodeType();        
        org.hl7.fhir.r4.model.Coding coding = new org.hl7.fhir.r4.model.Coding();
        org.hl7.fhir.r4.model.CodeableConcept codeableConcept = new org.hl7.fhir.r4.model.CodeableConcept();

        org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementSoftwareComponent cssc = new org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementSoftwareComponent();
        org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementImplementationComponent implementation = new org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementImplementationComponent();        
        org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestSecurityComponent security = new org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestSecurityComponent();
        org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestComponent rest = new org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestComponent();
        org.hl7.fhir.r4.model.CapabilityStatement cs = new org.hl7.fhir.r4.model.CapabilityStatement();
        List<org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceOperationComponent> restComponents = new ArrayList<>();

        String releaseDateSource = "05/01/2020 00:00:00";
        
        cs.setStatus(org.hl7.fhir.r4.model.Enumerations.PublicationStatus.DRAFT);
        cs.setFhirVersion(org.hl7.fhir.r4.model.Enumerations.FHIRVersion._4_0_0);
        cs.setPurpose("Defines FHIR R4 (V2) version of AB2D bulk data download");
        cs.setKind(org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementKind.REQUIREMENTS);

        cssc.setName("AB2D");
        cssc.setVersion("V2");
        implementation.setDescription("AB2D FHIR R4 Bulk Data Download Implementation");
        
        restComponents.add(createR4Operation("export", server + "/Patient/$export"));
        restComponents.add(createR4Operation("export by contract", server + "/Group/{contractNumber}/$export"));
        restComponents.add(createR4Operation("cancel", server + "/Job/{jobUuid}/$status"));
        restComponents.add(createR4Operation("status", server + "/Job/{jobUuid}/$status"));
        restComponents.add(createR4Operation("download", server + "/Job/{jobUuid}/file/{filename}"));
        restComponents.add(createR4Operation("capability", server + "/metadata"));

        rest.setMode(org.hl7.fhir.r4.model.CapabilityStatement.RestfulCapabilityMode.SERVER);
        rest.setInteraction(List.of(new org.hl7.fhir.r4.model.CapabilityStatement.SystemInteractionComponent().setCode(org.hl7.fhir.r4.model.CapabilityStatement.SystemRestfulInteraction.BATCH)));

        try {
            Date lastUpdated = sdf.parse("02/22/2020 00:00:00");
            cs.setDate(lastUpdated);
        } catch (Exception ex) {
            cs.setDate(new Date());
        }
        
        // Duplicate Code
        implementation.setUrl(server);
        security.setCors(true);
        security.setService(List.of(codeableConcept));
        rest.setSecurity(security);

        codeType.setValue(APPLICATION_JSON);
        codeType2.setValue("application/fhir+json");

        coding.setSystem("http://hl7.org/fhir/ValueSet/restful-security-service");
        coding.setCode(oauthString);
        coding.setDisplay(oauthString);

        codeableConcept.setCoding(List.of(coding));
        codeableConcept.setText(oauthString);

        try {
            Date releaseDate = sdf.parse(releaseDateSource);
            cssc.setReleaseDate(releaseDate);
        } catch (Exception ex) {
            cssc.setReleaseDate(new Date());
        }

        cs.setPublisher("Centers for Medicare &amp; Medicaid Services");
        cs.setFormat(List.of(codeType, codeType2));
        cs.setSoftware(cssc);
        cs.setImplementation(implementation);

        rest.setOperation(restComponents);
        cs.setRest(List.of(rest));

        return cs;
    }

    public static org.hl7.fhir.dstu3.model.CapabilityStatement generateSTU3CapabilityStatement(String server) {
        org.hl7.fhir.dstu3.model.CodeType codeType = new org.hl7.fhir.dstu3.model.CodeType();        
        org.hl7.fhir.dstu3.model.CodeType codeType2 = new org.hl7.fhir.dstu3.model.CodeType();        
        org.hl7.fhir.dstu3.model.Coding coding = new org.hl7.fhir.dstu3.model.Coding();
        org.hl7.fhir.dstu3.model.CodeableConcept codeableConcept = new org.hl7.fhir.dstu3.model.CodeableConcept();

        org.hl7.fhir.dstu3.model.CapabilityStatement.CapabilityStatementSoftwareComponent cssc = new org.hl7.fhir.dstu3.model.CapabilityStatement.CapabilityStatementSoftwareComponent();
        org.hl7.fhir.dstu3.model.CapabilityStatement.CapabilityStatementImplementationComponent implementation = new org.hl7.fhir.dstu3.model.CapabilityStatement.CapabilityStatementImplementationComponent();        
        org.hl7.fhir.dstu3.model.CapabilityStatement.CapabilityStatementRestSecurityComponent security = new org.hl7.fhir.dstu3.model.CapabilityStatement.CapabilityStatementRestSecurityComponent();
        org.hl7.fhir.dstu3.model.CapabilityStatement.CapabilityStatementRestComponent rest = new org.hl7.fhir.dstu3.model.CapabilityStatement.CapabilityStatementRestComponent();
        org.hl7.fhir.dstu3.model.CapabilityStatement cs = new org.hl7.fhir.dstu3.model.CapabilityStatement();
        List<org.hl7.fhir.dstu3.model.CapabilityStatement.CapabilityStatementRestOperationComponent> restComponents = new ArrayList<>();

        String releaseDateSource = "08/27/2020 00:00:00";
        
        cs.setStatus(org.hl7.fhir.dstu3.model.Enumerations.PublicationStatus.ACTIVE);
        cs.setAcceptUnknown(org.hl7.fhir.dstu3.model.CapabilityStatement.UnknownContentCode.EXTENSIONS);
        cs.setKind(org.hl7.fhir.dstu3.model.CapabilityStatement.CapabilityStatementKind.REQUIREMENTS);
        cs.setFhirVersion(FhirVersionEnum.DSTU3.getFhirVersionString());

        cssc.setName("AB2D");
        cssc.setVersion("V1");

        restComponents.add(createSTU3Operation("export", server + "/Patient/$export"));
        restComponents.add(createSTU3Operation("export by contract", server + "/Group/{contractNumber}/$export"));
        restComponents.add(createSTU3Operation("cancel", server + "/Job/{jobUuid}/$status"));
        restComponents.add(createSTU3Operation("status", server + "/Job/{jobUuid}/$status"));
        restComponents.add(createSTU3Operation("download", server + "/Job/{jobUuid}/file/{filename}"));
        restComponents.add(createSTU3Operation("capability", server + "/metadata"));

        rest.setMode(org.hl7.fhir.dstu3.model.CapabilityStatement.RestfulCapabilityMode.SERVER);
        rest.setInteraction(List.of(new org.hl7.fhir.dstu3.model.CapabilityStatement.SystemInteractionComponent().setCode(org.hl7.fhir.dstu3.model.CapabilityStatement.SystemRestfulInteraction.BATCH)));

        String dt = sdfLess.format(new Date());
        try {
            cs.setDate(sdfLess.parse(dt));
        } catch (ParseException e) {
            cs.setDate(new Date());
        }

        // Duplicate Code
        implementation.setUrl(server);
        security.setCors(true);
        security.setService(List.of(codeableConcept));
        rest.setSecurity(security);

        codeType.setValue(APPLICATION_JSON);
        codeType2.setValue("application/fhir+json");

        coding.setSystem("http://hl7.org/fhir/ValueSet/restful-security-service");
        coding.setCode(oauthString);
        coding.setDisplay(oauthString);

        codeableConcept.setCoding(List.of(coding));
        codeableConcept.setText(oauthString);

        try {
            Date releaseDate = sdf.parse(releaseDateSource);
            cssc.setReleaseDate(releaseDate);
        } catch (Exception ex) {
            cssc.setReleaseDate(new Date());
        }

        cs.setPublisher("Centers for Medicare &amp; Medicaid Services");
        cs.setFormat(List.of(codeType, codeType2));
        cs.setSoftware(cssc);
        cs.setImplementation(implementation);

        rest.setOperation(restComponents);
        cs.setRest(List.of(rest));

        return cs;

    }
    
    private static org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceOperationComponent createR4Operation(String name, String path) {
        org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceOperationComponent operation = new org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceOperationComponent();
        operation.setName(name);
        operation.setDefinition(path);
        return operation;
    }

    private static org.hl7.fhir.dstu3.model.CapabilityStatement.CapabilityStatementRestOperationComponent createSTU3Operation(String name, String path) {
        org.hl7.fhir.dstu3.model.CapabilityStatement.CapabilityStatementRestOperationComponent operation = new org.hl7.fhir.dstu3.model.CapabilityStatement.CapabilityStatementRestOperationComponent();
        operation.setName(name);
        org.hl7.fhir.dstu3.model.Reference ref = new org.hl7.fhir.dstu3.model.Reference();
        ref.setReference(path);
        operation.setDefinition(ref);
        return operation;
    }
}