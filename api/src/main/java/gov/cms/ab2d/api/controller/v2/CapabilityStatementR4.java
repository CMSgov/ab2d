package gov.cms.ab2d.api.controller.v2;

import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static gov.cms.ab2d.api.controller.common.ApiText.APPLICATION_JSON;

public class CapabilityStatementR4 {
    public static CapabilityStatement populateCS(String server) {
        String OAUTH_STRING = "OAuth";
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        CapabilityStatement cs = new CapabilityStatement();
        cs.setPublisher("Centers for Medicare &amp; Medicaid Services");
        cs.setKind(CapabilityStatement.CapabilityStatementKind.REQUIREMENTS);
        cs.setStatus(Enumerations.PublicationStatus.DRAFT);
        try {
            Date lastUpdated = sdf.parse("02/22/2020 00:00:00");
            cs.setDate(lastUpdated);
        } catch (Exception ex) {
            cs.setDate(new Date());
        }
        cs.setFhirVersion(Enumerations.FHIRVersion._4_0_0);
        cs.setPurpose("Defines FHIR R4 (V2) version of AB2D bulk data download");

        CodeType codeType = new CodeType();
        codeType.setValue(APPLICATION_JSON);
        CodeType codeType2 = new CodeType();
        codeType2.setValue("application/fhir+json");
        cs.setFormat(List.of(codeType, codeType2));

        CapabilityStatement.CapabilityStatementSoftwareComponent cssc = new CapabilityStatement.CapabilityStatementSoftwareComponent();
        cssc.setName("AB2D");
        cssc.setVersion("V2");
        try {
            Date releaseDate = sdf.parse("05/01/2020 00:00:00");
            cssc.setReleaseDate(releaseDate);
        } catch (Exception ex) {
            cssc.setReleaseDate(new Date());
        }
        cs.setSoftware(cssc);

        CapabilityStatement.CapabilityStatementImplementationComponent implementation = new CapabilityStatement.CapabilityStatementImplementationComponent();
        implementation.setDescription("AB2D FHIR R4 Bulk Data Download Implementation");
        implementation.setUrl(server);
        cs.setImplementation(implementation);

        CapabilityStatement.CapabilityStatementRestComponent rest = new CapabilityStatement.CapabilityStatementRestComponent();
        rest.setMode(CapabilityStatement.RestfulCapabilityMode.SERVER);
        CapabilityStatement.CapabilityStatementRestSecurityComponent security = new CapabilityStatement.CapabilityStatementRestSecurityComponent();
        security.setCors(true);

        CodeableConcept codeableConcept = new CodeableConcept();
        Coding coding = new Coding();
        coding.setSystem("http://hl7.org/fhir/ValueSet/restful-security-service");
        coding.setCode(OAUTH_STRING);
        coding.setDisplay(OAUTH_STRING);
        codeableConcept.setCoding(List.of(coding));
        codeableConcept.setText(OAUTH_STRING);
        security.setService(List.of(codeableConcept));
        rest.setSecurity(security);
        List<CapabilityStatement.CapabilityStatementRestResourceOperationComponent> restComponents = new ArrayList<>();
        restComponents.add(createOperation("export", server + "/Patient/$export"));
        restComponents.add(createOperation("export by contract", server + "/Group/{contractNumber}/$export"));
        restComponents.add(createOperation("cancel", server + "/Job/{jobUuid}/$status"));
        restComponents.add(createOperation("status", server + "/Job/{jobUuid}/$status"));
        restComponents.add(createOperation("download", server + "/Job/{jobUuid}/file/{filename}"));
        restComponents.add(createOperation("capability", server + "/metadata"));
        rest.setOperation(restComponents);
        rest.setInteraction(List.of(new CapabilityStatement.SystemInteractionComponent().setCode(CapabilityStatement.SystemRestfulInteraction.BATCH)));
        cs.setRest(List.of(rest));
        return cs;
    }

    private static CapabilityStatement.CapabilityStatementRestResourceOperationComponent createOperation(String name, String path) {
        CapabilityStatement.CapabilityStatementRestResourceOperationComponent operation = new CapabilityStatement.CapabilityStatementRestResourceOperationComponent();
        operation.setName(name);
        operation.setDefinition(path);
        return operation;
    }
}
